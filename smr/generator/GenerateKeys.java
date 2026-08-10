import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateKeys {

    public static void main(String[] args) throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        String privateKey = """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(keyPair.getPrivate().getEncoded())
        );

        String publicKey = """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(keyPair.getPublic().getEncoded())
        );

        Files.writeString(
                Path.of("private.key"),
                privateKey,
                StandardCharsets.UTF_8
        );

        Files.writeString(
                Path.of("public.key"),
                publicKey,
                StandardCharsets.UTF_8
        );

        System.out.println("RSA 2048 key pair generated successfully.");
        System.out.println("private.key 🔐");
        System.out.println("public.key  🔓");
    }
}