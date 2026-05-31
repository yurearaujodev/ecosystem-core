package br.com.yat.ecosystemcore.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.yat.ecosystemcore.domain.enums.CryptoErrorType;

/**
 * Classe utilitária para operações de leitura e escrita de arquivos binários e de texto.
 * <p>
 * Integra:
 * <ul>
 * <li>{@link java.nio.file.Files} para manipulação de arquivos.</li>
 * <li>{@link StandardOpenOption} para opções de escrita seguras.</li>
 * <li>{@link CryptoException} para tratamento de erros de I/O.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class FileManager {

	private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	private static final StandardOpenOption[] WRITE_OPTIONS = { StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE };

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private FileManager() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Salva dados binários em um arquivo.
	 * 
	 * @param file caminho do arquivo destino
	 * @param data dados binários a serem salvos
	 * @throws CryptoException se ocorrer falha ao salvar o arquivo
	 */
	public static void save(Path file, byte[] data) {
		Objects.requireNonNull(file, "file não pode ser nula");
		Objects.requireNonNull(data, "data não pode ser nula");

		try {
			prepareParentDirectory(file);

			Files.write(file, data, WRITE_OPTIONS);
			logger.debug("Arquivo binário salvo com sucesso: {}", file);
		} catch (Exception e) {
			logger.error("Falha ao salvar arquivo binário: {}", file, e);
			throw new CryptoException(CryptoErrorType.IO_ERROR, "Falha ao salvar arquivo: " + file, e);
		}
	}

	/**
	 * Carrega dados binários de um arquivo.
	 * 
	 * @param file caminho do arquivo origem
	 * @return dados binários carregados
	 * @throws CryptoException se ocorrer falha ao carregar o arquivo
	 */
	public static byte[] load(Path file) {
		Objects.requireNonNull(file, "file não pode ser nula");

		try {
			validateReadableFile(file);
			byte[] data = Files.readAllBytes(file);
			logger.debug("Arquivo binário carregado com sucesso: {}", file);
			return data;
		} catch (Exception e) {
			logger.error("Falha ao carregar arquivo binário: {}", file, e);
			throw new CryptoException(CryptoErrorType.IO_ERROR, "Falha ao carregar arquivo: " + file, e);
		}
	}

	/**
	 * Salva texto em um arquivo utilizando codificação UTF-8.
	 * 
	 * @param file caminho do arquivo destino
	 * @param text texto a ser salvo
	 * @throws CryptoException se ocorrer falha ao salvar o arquivo
	 */
	public static void saveText(Path file, String text) {
		Objects.requireNonNull(file, "file não pode ser nula");
		Objects.requireNonNull(text, "text não pode ser nula");
		try {

			prepareParentDirectory(file);

			Files.writeString(file, text, StandardCharsets.UTF_8, WRITE_OPTIONS);
			logger.debug("Arquivo de texto salvo: {}", file);
		} catch (Exception e) {
			logger.error("Falha ao salvar arquivo de texto: {}", file, e);
			throw new CryptoException(CryptoErrorType.IO_ERROR, "Falha ao salvar texto em arquivo: " + file, e);
		}
	}

	/**
	 * Carrega texto de um arquivo utilizando codificação UTF-8.
	 * 
	 * @param file caminho carregado do arquivo origem
	 * @return texto carregado
	 * @throws CryptoException se ocorrer falha ao carregar o arquivo
	 */
	public static String loadText(Path file) {
		Objects.requireNonNull(file, "file não pode ser nula");

		try {
			validateReadableFile(file);

			String text = Files.readString(file, StandardCharsets.UTF_8);
			logger.debug("Arquivo de texto carregado com sucesso: {}", file);
			return text;
		} catch (Exception e) {
			logger.error("Falha ao carregar arquivo de texto: {}", file, e);
			throw new CryptoException(CryptoErrorType.IO_ERROR, "Falha ao carregar texto de arquivo: " + file, e);
		}
	}

	/**
	 * Garante que o diretório pai do arquivo existe e possui permissão de escrita
	 * 
	 * @param file caminho do arquivo
	 * @throws IOException se não houver permissão ou falha na criação do diretório
	 */
	private static void prepareParentDirectory(Path file) throws IOException {
		Path parent = file.getParent();

		if (parent != null) {
			if (!Files.exists(parent)) {
				Files.createDirectories(parent);
			}
		}

		if (!Files.isWritable(parent)) {
			throw new IOException("Sem permissão de escrita no diretório: " + parent);
		}
	}

	/**
	 * Valida se o arquivo existe, é regular e possui permissão de leitura.
	 * 
	 * @param file caminho do arquivo
	 * @throws IOException se o arquivo não for válido ou não puder ser lido
	 */
	private static void validateReadableFile(final Path file) throws IOException {
		if (!Files.exists(file)) {
			throw new IOException("Arquivo não encontrado: " + file);
		}

		if (!Files.isRegularFile(file)) {
			throw new IOException("Caminho não é um arquivo regular: " + file);
		}

		if (!Files.isReadable(file)) {
			throw new IOException("Sem permissão de leitura no arquivo: " + file);
		}
	}

}
