package br.com.yat.ecosystemcore.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public final class QrCodeGenerator {

    private QrCodeGenerator() {}

    /**
     * Gera um QR Code real, matemático e 100% legível pelo celular.
     * * @param conteudo O link otpauth:// gerado pelo GoogleAuth
     * @param tamanho Largura e altura da imagem em pixels
     * @return WritableImage pronta para ser usada no ImageView do JavaFX
     */
    public static WritableImage gerarImagemQrCode(String conteudo, int tamanho) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            
            // Configurações de codificação e nível de correção de erro (M = Médio, ideal para telas)
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2); // Adiciona uma margem branca de segurança ao redor

            // 🌟 O ZXing calcula as posições exatas dos blocos de dados e sincronismo
            BitMatrix bitMatrix = qrCodeWriter.encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho, hints);

            // Transforma a matriz calculada em uma imagem real do JavaFX
            WritableImage img = new WritableImage(tamanho, tamanho);
            PixelWriter pw = img.getPixelWriter();

            for (int y = 0; y < tamanho; y++) {
                for (int x = 0; x < tamanho; x++) {
                    // Se o bit da matriz for verdadeiro pinta de preto, senão de branco
                    Color cor = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                    pw.setColor(x, y, cor);
                }
            }

            return img;
            
        } catch (Exception e) {
            throw new RuntimeException("Falha crítica ao calcular e gerar os blocos do QR Code.", e);
        }
    }
}