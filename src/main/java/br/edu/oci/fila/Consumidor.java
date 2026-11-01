
package br.edu.oci.fila;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.UpdateMessageDetails;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.requests.UpdateMessageRequest;

import java.util.List;
import java.util.Scanner;

public class Consumidor {

    // Quanto tempo (s) a mensagem fica invisível para outros consumidores
    // enquanto esta aplicação tenta processá-la.
    private static final int VISIBILITY_IN_SECONDS = 30;

    // Long polling: quanto tempo (s) o GetMessages aguarda por mensagens
    // antes de responder vazio.
    private static final int POLLING_TIMEOUT_IN_SECONDS = 10;

    // Quantas mensagens buscar por chamada (máximo permitido pela OCI é 20).
    private static final int MAX_MENSAGENS_POR_CHAMADA = 10;

    // Visibilidade (s) ao devolver uma mensagem que falhou no processamento.
    // 0 = fica disponível para nova tentativa imediatamente.
    private static final int VISIBILITY_AO_FALHAR_EM_SEGUNDOS = 0;

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== CONSUMIDOR OCI QUEUE ===");

        System.out.print("Canal para consumir (Enter para consumir de todos os canais): ");
        String canal = scanner.nextLine().trim();
        String channelFilter = canal.isBlank() ? null : canal;

        String queueId = System.getenv("QUEUE_ID");

        if (queueId == null || queueId.isBlank()) {
            throw new IllegalStateException("QUEUE_ID não configurado.");
        }

        ObjectMapper mapper = new ObjectMapper();

        System.out.println("\nConectando à OCI Queue...");

        try (QueueClient cliente = ConexaoOci.conectar()) {

            System.out.println("Aguardando mensagens" +
                    (channelFilter != null ? " no canal '" + channelFilter + "'" : "") +
                    "... (Ctrl+C para parar)\n");

            while (true) {
                processarProximoLote(cliente, queueId, channelFilter, mapper);
            }
        }
    }

    private static void processarProximoLote(
            QueueClient cliente,
            String queueId,
            String channelFilter,
            ObjectMapper mapper
    ) {
        GetMessagesRequest.Builder requisicao = GetMessagesRequest.builder()
                .queueId(queueId)
                .visibilityInSeconds(VISIBILITY_IN_SECONDS)
                .timeoutInSeconds(POLLING_TIMEOUT_IN_SECONDS)
                .limit(MAX_MENSAGENS_POR_CHAMADA);

        if (channelFilter != null) {
            requisicao.channelFilter(channelFilter);
        }

        List<GetMessage> mensagens = cliente.getMessages(requisicao.build())
                .getGetMessages()
                .getMessages();

        if (mensagens == null || mensagens.isEmpty()) {
            return;
        }

        for (GetMessage mensagem : mensagens) {
            processarMensagem(cliente, queueId, mensagem, mapper);
        }
    }

    private static void processarMensagem(
            QueueClient cliente,
            String queueId,
            GetMessage mensagem,
            ObjectMapper mapper
    ) {
        String canalOrigem = mensagem.getMetadata() != null
                ? mensagem.getMetadata().getChannelId()
                : null;

        try {
            MensagemEmail email = mapper.readValue(mensagem.getContent(), MensagemEmail.class);

            System.out.println("Processando mensagem " + mensagem.getId() +
                    (canalOrigem != null ? " (canal: " + canalOrigem + ")" : "") +
                    " - tentativa " + mensagem.getDeliveryCount());

            EnviadorEmail.enviar(email);

            excluirMensagem(cliente, queueId, mensagem);

            System.out.println("Mensagem " + mensagem.getId() + " processada e removida da fila.\n");

        } catch (Exception e) {

            System.out.println("Falha ao processar mensagem " + mensagem.getId() +
                    ": " + e.getMessage());

            devolverParaFila(cliente, queueId, mensagem);

            System.out.println("Mensagem " + mensagem.getId() +
                    " devolvida à fila para nova tentativa.\n");
        }
    }

    private static void excluirMensagem(QueueClient cliente, String queueId, GetMessage mensagem) {
        cliente.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueId(queueId)
                        .messageReceipt(mensagem.getReceipt())
                        .build()
        );
    }

    private static void devolverParaFila(QueueClient cliente, String queueId, GetMessage mensagem) {
        try {
            cliente.updateMessage(
                    UpdateMessageRequest.builder()
                            .queueId(queueId)
                            .messageReceipt(mensagem.getReceipt())
                            .updateMessageDetails(
                                    UpdateMessageDetails.builder()
                                            .visibilityInSeconds(VISIBILITY_AO_FALHAR_EM_SEGUNDOS)
                                            .build()
                            )
                            .build()
            );
        } catch (Exception e) {
            // Se não conseguirmos atualizar a visibilidade, a mensagem volta
            // a ficar disponível sozinha quando VISIBILITY_IN_SECONDS expirar.
            System.out.println("Não foi possível atualizar a visibilidade da mensagem " +
                    mensagem.getId() + ": " + e.getMessage());
        }
    }
}
