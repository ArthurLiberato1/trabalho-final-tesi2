
package br.edu.oci.fila;

public class EnviadorEmail {

    public static void enviar(MensagemEmail email) throws Exception {

        // Este projeto não integra com um servidor SMTP de verdade: para
        // testar o tratamento de falhas do Consumidor, basta incluir a
        // palavra "falha" no corpo da mensagem ao usar o Produtor.
        if (email.mensagem().toLowerCase().contains("falha")) {
            throw new Exception("Falha simulada no envio para " + email.destinatario());
        }

        System.out.println("  -> E-mail enviado para " + email.destinatario() +
                " | Assunto: " + email.assunto());
    }
}
