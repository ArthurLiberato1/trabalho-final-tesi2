package br.edu.oci.fila;

public record MensagemEmail(
        String id,
        String destinatario,
        String assunto,
        String mensagem
) {

    public MensagemEmail {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "O ID é obrigatório."
            );
        }

        if (destinatario == null ||
                !destinatario.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException(
                    "O destinatário deve ser um e-mail válido."
            );
        }

        if (assunto == null || assunto.isBlank()) {
            throw new IllegalArgumentException(
                    "O assunto é obrigatório."
            );
        }

        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException(
                    "O corpo da mensagem é obrigatório."
            );
        }
    }
}
