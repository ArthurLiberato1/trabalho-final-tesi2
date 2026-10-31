
package br.edu.oci.fila;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.*;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import com.oracle.bmc.queue.responses.PutMessagesResponse;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Produtor {

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== PRODUTOR OCI QUEUE ===");

        // 1. Receber dados
        System.out.print("Destinatário: ");
        String destinatario = scanner.nextLine();

        System.out.print("Assunto: ");
        String assunto = scanner.nextLine();

        System.out.print("Mensagem: ");
        String corpo = scanner.nextLine();

        System.out.print("Canal (Enter para não utilizar): ");
        String canal = scanner.nextLine().trim();

        // 2. Criar e validar objeto Java
        MensagemEmail email = new MensagemEmail(
                UUID.randomUUID().toString(),
                destinatario,
                assunto,
                corpo
        );

        // 3. Converter objeto para JSON
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(email);

        System.out.println("\nJSON gerado:");
        System.out.println(json);

        // 4. Recuperar OCID da fila
        String queueId = System.getenv("QUEUE_ID");

        if (queueId == null || queueId.isBlank()) {
            throw new IllegalStateException(
                    "QUEUE_ID não configurado."
            );
        }

        // 5. Criar mensagem para a OCI
        PutMessagesDetailsEntry.Builder entrada =
                PutMessagesDetailsEntry.builder()
                        .content(json);

        if (!canal.isBlank()) {
            entrada.metadata(
                    MessageMetadata.builder()
                            .channelId(canal)
                            .build()
            );
        }

        PutMessagesDetails detalhes =
                PutMessagesDetails.builder()
                        .messages(List.of(entrada.build()))
                        .build();

        // 6. Montar requisição
        PutMessagesRequest requisicao =
                PutMessagesRequest.builder()
                        .queueId(queueId)
                        .putMessagesDetails(detalhes)
                        .build();

        // 7. Enviar para a OCI
        System.out.println("\nConectando à OCI Queue...");

        try (QueueClient cliente = ConexaoOci.conectar()) {

            PutMessagesResponse resposta =
                    cliente.putMessages(requisicao);

            var mensagens =
                    resposta.getPutMessages().getMessages();

            if (mensagens == null || mensagens.isEmpty()) {
                throw new IllegalStateException(
                        "A OCI não confirmou a publicação."
                );
            }

            System.out.println("\nMensagem publicada com sucesso!");
            System.out.println(
                    "ID da mensagem OCI: " + mensagens.get(0).getId()
            );
            System.out.println(
                    "ID da requisição: " + resposta.getOpcRequestId()
            );
        }
    }
}
