
package br.edu.oci.fila;

public class EnviadorEmail {

    /**
     * Simula o disparo de um e-mail. Não há integração real com um servidor
     * SMTP neste projeto de demonstração: para testar o tratamento de falhas
     * do consumidor, inclua a palavra "falha" no corpo da mensagem ao usar
     * o Produtor.
     */
    public static void enviar(MensagemEmail email) throws Exception {

        if (email.mensagem().toLowerCase().contains("falha")) {
            throw new Exception("Falha simulada no envio para " + email.destinatario());
        }

        System.out.println("  -> E-mail enviado para " + email.destinatario() +
                " | Assunto: " + email.assunto());
    }
}
