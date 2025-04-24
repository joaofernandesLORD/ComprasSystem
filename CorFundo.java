import java.awt.Color;

public class CorFundo {
    public static final Color FUNDO_PRINCIPAL = new Color(184, 184, 184); // Cinza claro
    public static final Color FUNDO_SECUNDARIO = new Color(156, 154, 154); // Cinza médio
    public static final Color FUNDO_TERCEIRO = new Color(111, 111, 111); // Cinza escuro
    public static final Color FUNDO_PADRAO = new Color(255, 255, 255); // Branco

    public static Color getCorFundo(String nomeCor) {
        try {
            if (nomeCor == null) {
                return FUNDO_PADRAO;
            }

            switch (nomeCor.toLowerCase()) {
                case "fundo principal":
                    return FUNDO_PRINCIPAL;
                case "fundo secundario":
                    return FUNDO_SECUNDARIO;
                case "fundo terceiro":
                    return FUNDO_TERCEIRO;
                default:
                    return FUNDO_PADRAO;
            }
        } catch (NullPointerException e) {
            return FUNDO_PADRAO;
        }
    }

    public static Color getCorPersonalizada(int r, int g, int b) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("Valores RGB devem estar entre 0 e 255");
        }
        return new Color(r, g, b);
    }
}