
package br.edu.oci.fila;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;

public class ConexaoOci {

    public static QueueClient conectar() throws Exception {

        // Recupera o endpoint das variáveis de ambiente
        String endpoint =
                System.getenv("QUEUE_MESSAGES_ENDPOINT");

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(
                    "QUEUE_MESSAGES_ENDPOINT não configurado."
            );
        }

        // Carrega o arquivo ~/.oci/config
        var config = ConfigFileReader.parseDefault();

        // Autentica utilizando a chave privada
        var autenticacao =
                new ConfigFileAuthenticationDetailsProvider(config);

        // Cria o cliente oficial OCI Queue
        QueueClient cliente =
                QueueClient.builder().build(autenticacao);

        // Define o endpoint de mensagens
        cliente.setEndpoint(endpoint);

        return cliente;
    }
}
