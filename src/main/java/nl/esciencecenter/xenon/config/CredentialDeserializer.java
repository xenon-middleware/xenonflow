package nl.esciencecenter.xenon.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.DefaultCredential;
import nl.esciencecenter.xenon.credentials.PasswordCredential;
import nl.esciencecenter.xenon.credentials.CertificateCredential;

public class CredentialDeserializer extends JsonDeserializer<Credential> {

	@Override
	public Credential deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		
		if(!node.has("user")) {
			throw new JsonMappingException(p, "User field is required for credentials");
		}
		String user = node.get("user").asText();
		
		if(node.has("password")) {
			char[] password = decodeBinaryValue(node.get("password"));					
			return new PasswordCredential(user, password);
		} else if(node.has("certificatefile")){
			String certificateFile = node.get("certificatefile").asText();
			char[] passphrase = null;
			
			if(node.has("passphrase")) {
				passphrase = decodeBinaryValue(node.get("passphrase"));
			}
			return new CertificateCredential(user, certificateFile, passphrase);
		} else {
			return new DefaultCredential(user);
		}
	}
	
	/**
	 * Decode base64 encoded utf-8 strings from JsonNodes
	 * 
	 * Used for password to avoid the String object
	 * 
	 * @param node
	 * @return Decoded binary value
	 * @throws IOException
	 */
	private static char[] decodeBinaryValue(JsonNode node) throws IOException {
		byte[] bytes = node.binaryValue();
		Charset utf8 = Charset.forName("UTF-8");

		CharBuffer characters = utf8.decode(ByteBuffer.wrap(bytes));
		char[] chars = new char[characters.remaining()]; 
		characters.get(chars);
		
		return chars;
	}

}
