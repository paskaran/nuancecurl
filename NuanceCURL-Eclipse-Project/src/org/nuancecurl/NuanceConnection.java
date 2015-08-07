package org.nuancecurl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * API for connecting to Nuance Dragon Dictation Service.
 * 
 * @author Dinesh Paskaran &copy; 2015
 *
 */
public class NuanceConnection {

	/**
	 * The base URL for the API
	 */
	private final String NUANCE_HOST_SERVER;

	/**
	 * Stores listeners
	 */
	private List<NuanceResponseListener> responseListeners = new ArrayList<NuanceResponseListener>();

	/**
	 * User defined API-KEY
	 */
	private final String API_KEY;
	/**
	 * User defined API-ID
	 */
	private final String API_ID;

	/**
	 * Random ID
	 */
	private final String RANDOM_ID;
	/**
	 * User-defined language
	 */
	private final String LANGUAGE;
	/**
	 * Topic for better recognition
	 */
	private final String TOPIC;

	/**
	 * Content-Type header information
	 */
	private final String CONTENT_TYPE;

	/**
	 * ShellScript to run CURL
	 */
	private final File SHELL_SCRIPT;

	private static final char[] CHARS_FOR_ID = "ABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789_abcdefghijklmnopqrstuvwxyz"
			.toCharArray();

	/**
	 * Constructor with credentials for connecting to the
	 * Nuance-Dictation-Service
	 * 
	 * @param hostServer
	 *            Server URI with Servlet path exp.
	 *            <code>https://dictation.nuancemobility.net:443/NMDPAsrCmdServlet/dictation</code>
	 *            note to add the port within the URL
	 * @param apiKey
	 *            The API key or APP key from your registration to Nuance
	 *            Developer Page. Note to add the key as String representation.
	 * @param apiId
	 *            The API ID or APP id from your registration to Nuance
	 *            Developer Page
	 * @param randomId
	 *            Random Id for transcription. See developer guide for mor info.
	 *            See also NuanceConnection.createRandomId().
	 * @param language
	 *            The language of the transcription.
	 * @param topic
	 *            The topic of the transcription.
	 * @param contentType
	 *            The ContentType of the used speech file.
	 * @param shellScript
	 *            Path to the shellscript for calling the CURL command.
	 */
	public NuanceConnection(String hostServer, String apiKey,
			String apiId, String randomId, String language,
			String topic, String contentType, File shellScript) {
		this.NUANCE_HOST_SERVER = hostServer;
		this.API_KEY = apiKey;
		this.API_ID = apiId;
		this.RANDOM_ID = randomId;
		this.LANGUAGE = language;
		this.TOPIC = topic;
		this.CONTENT_TYPE = contentType;
		this.SHELL_SCRIPT = shellScript;
	}

	/**
	 * Send a FLAC file with the specified sampleRate to the Duplex API
	 * 
	 * @param file
	 *            The file you wish to upload. NOTE: Segment the file if
	 *            duration is greater than 15 seconds.
	 * @throws IOException
	 *             If something has gone wrong with reading the file
	 */
	public void recognize(File file) throws IOException {

		ProcessBuilder pb = new ProcessBuilder(
				SHELL_SCRIPT.getAbsolutePath(), NUANCE_HOST_SERVER,
				API_ID, API_KEY, RANDOM_ID, CONTENT_TYPE, LANGUAGE,
				TOPIC, file.getAbsolutePath());

		System.out.println(pb.command());
		pb.directory(SHELL_SCRIPT.getParentFile());
		pb.redirectErrorStream(true);
		Process p = pb.start();
		InputStream is = p.getInputStream();

		BufferedInputStream bis = new BufferedInputStream(is);
		byte[] contents = new byte[1024];

		int bytesRead = 0;
		String strFileContents = "";
		StringBuffer sb = new StringBuffer();
		while ((bytesRead = bis.read(contents)) != -1) {
			strFileContents = new String(contents, 0, bytesRead);
			sb.append(strFileContents);
		}
		String response = sb.toString();
		NuanceResponse nr = null;
		if (!response.contains("<title>Error")) {
			nr = parseTranscript(response);
		} else {
			nr = new NuanceResponse();
			nr.errorCode = sb.toString();
		}
		fireResponseEvent(nr);
	}

	/**
	 * Method create a random id for the transcription. See developer guide of
	 * NuanceDragon HTTP SDK.
	 * 
	 * @return An ID with 32-Characters
	 */
	public static String createRandomId() {
		StringBuffer sb = new StringBuffer();
		for (int i = 1; i < 33; i++) {
			char c = CHARS_FOR_ID[((int) (Math.random() * i
					* CHARS_FOR_ID.length)) % CHARS_FOR_ID.length];
			sb.append(c);
		}

		return sb.toString();
	}

	/**
	 * Parses each individual "transcript" phrase
	 * 
	 * @param The
	 *            string fragment to parse
	 * @return NuanceResponse - Object containing the hypothesis of the
	 *         transcription.
	 */
	private NuanceResponse parseTranscript(String s) {
		String[] rsp = s.split("\n");
		NuanceResponse nr = new NuanceResponse();
		nr.hypothesis.addAll(Arrays.asList(rsp));
		return nr;
	}

	/**
	 * Adds NuanceResponse Listeners that fire when a response was retrieved
	 * from curl command.
	 * 
	 * @param The
	 *            Listeners you want to add
	 */
	public void addResponseListener(NuanceResponseListener rl) {
		responseListeners.add(rl);
	}

	/**
	 * RemovesNuanceResponse Listeners that fire when a response was retrieved
	 * 
	 * @param rl
	 *            The Listeners you want to remove
	 */
	public void removeResponseListener(NuanceResponseListener rl) {
		responseListeners.remove(rl);
	}

	/**
	 * Fires responseListeners
	 * 
	 * @param nr
	 *            The Nuance Response.
	 */
	private synchronized void fireResponseEvent(NuanceResponse nr) {
		for (NuanceResponseListener nrl : responseListeners) {
			nrl.onResponse(nr);
		}
	}

	/**
	 * Response listeners for Nuance Connection
	 */
	public interface NuanceResponseListener {
		/**
		 * The OnResponse method is called when the response from server could
		 * retrieved. Check before accessing the hypothesis list with the method
		 * <code>nr.wasSuccessful()</code>. If this method returns
		 * <code>true</code> then the transcription was successful otherwise
		 * this method returns <code>false</code> Then you can access the error
		 * from the server using <code>nr.getError()</code> method.
		 * 
		 * @param nr
		 *            The returned response object.
		 */
		public void onResponse(NuanceResponse nr);

	}

	/**
	 * Class to model a response from server. Contains the list of hypothesis
	 * from the transcription and also errors if some occurred.
	 * 
	 * @author Dinesh Paskaran &copy; 2015
	 *
	 */
	public class NuanceResponse {
		private List<String> hypothesis;
		private String errorCode = "";

		/**
		 * Constructor of NuanceResponse
		 */
		public NuanceResponse() {
			this.hypothesis = new ArrayList<String>();
		}

		/**
		 * Returns the list of hypothesis as String-Objects. See also
		 * wasSuccessful()-method.
		 * 
		 * @return List of Hypothesis
		 */
		public List<String> getHypothesis() {
			return this.hypothesis;
		}

		/**
		 * Method returns true if transcription was successful otherwise false
		 * 
		 * @return true when transcription was successful
		 */
		public boolean wasSuccessful() {
			return errorCode.isEmpty();
		}

		/**
		 * Return error code when some occurred during transcription.
		 * 
		 * @return String
		 */
		public String getError() {
			return errorCode;
		}

	}
}
