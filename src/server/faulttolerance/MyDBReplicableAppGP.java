package server.faulttolerance; //export JAVA_HOME=$(/usr/libexec/java_home -v 1.8.0_292)

import com.datastax.driver.core.*;

import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.nio.interfaces.NodeConfig;
import edu.umass.cs.nio.nioutils.NIOHeader;
import edu.umass.cs.nio.nioutils.NodeConfigUtils;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.text.TableView.TableRow;

/**
 * This class should implement your {@link Replicable} database app if you wish
 * to use Gigapaxos.
 * <p>
 * Make sure that both a single instance of Cassandra is running at the default
 * port on localhost before testing.
 * <p>
 * Tips:
 * <p>
 * 1) No server-server communication is permitted or necessary as you are using
 * gigapaxos for all that.
 * <p>
 * 2) A {@link Replicable} must be agnostic to "myID" as it is a standalone
 * replication-agnostic application that via its {@link Replicable} interface is
 * being replicated by gigapaxos. However, in this assignment, we need myID as
 * each replica uses a different keyspace (because we are pretending different
 * replicas are like different keyspaces), so we use myID only for initiating
 * the connection to the backend data store.
 * <p>
 * 3) This class is never instantiated via a main method. You can have a main
 * method for your own testing purposes but it won't be invoked by any of
 * Grader's tests.
 */
public class MyDBReplicableAppGP implements Replicable {

	/**
	 * Set this value to as small a value with which you can get tests to still
	 * pass. The lower it is, the faster your implementation is. Grader* will
	 * use this value provided it is no greater than its MAX_SLEEP limit.
	 * Faster
	 * is not necessarily better, so don't sweat speed. Focus on safety.
	 */
	public static final int SLEEP = 1000;
	protected final String myID;
	final private Session session;
    final private Cluster cluster;
	/**
	 * All Gigapaxos apps must either support a no-args constructor or a
	 * constructor taking a String[] as the only argument. Gigapaxos relies on
	 * adherence to this policy in order to be able to reflectively construct
	 * customer application instances.
	 *
	 * @param args Singleton array whose args[0] specifies the keyspace in the
	 *             backend data store to which this server must connect.
	 *             Optional args[1] and args[2]
	 * @throws IOException
	 */
	public MyDBReplicableAppGP(String[] args) throws IOException {
		this.myID = args [0];
		session = (cluster=Cluster.builder().addContactPoint("127.0.0.1")
                .build()).connect(myID);
		//throw new RuntimeException("Not yet implemented");
	}

	/**
	 * Refer documentation of {@link Replicable#execute(Request, boolean)} to
	 * understand what the boolean flag means.
	 * <p>
	 * You can assume that all requests will be of type {@link
	 * edu.umass.cs.gigapaxos.paxospackets.RequestPacket}.
	 *
	 * @param request
	 * @param b
	 * @return
	 */
	@Override
	public boolean execute(Request request, boolean b) {
		return this.execute(request);
	}

	/**
	 * Refer documentation of
	 * {@link edu.umass.cs.gigapaxos.interfaces.Application#execute(Request)}
	 *
	 * @param request
	 * @return
	 */
	@Override
	public boolean execute(Request request) {
		//System.out.println("exec");
		String query = request.toString();
		System.out.println("EXECUTE ---------------------");
		JSONObject json;
		String req;
		try {
			json = new JSONObject(query);
			req = json.getString("QV");
			//System.out.println("REQ: "+req);
			session.execute(req);
			//checkpoint(req);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//ResultSet resultSet = session.execute(query);
		

		//session.execute(query);
		return true;
	}

	/**
	 * Refer documentation of {@link Replicable#checkpoint(String)}.
	 *
	 * @param s
	 * @return
	 */
	@Override
	public String checkpoint(String s) {
		System.out.println("checkpoint "+s);
		// TODO:
		ResultSet result = session.execute("SELECT * FROM grade;");
		String all_results = result.all().toString();
		// put string into a file
		// String filePath = "/Users/vaishnavishah/Documents/COMPSCI578/PA3.2/fault-tolerant-db-gp/store.txt";

        // try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
        //     // Write the string to the file
        //     writer.write(all_results);

        //     //System.out.println("String has been written to the file.");
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
		return all_results;//"true";
	}


	/**
	 * Refer documentation of {@link Replicable#restore(String, String)}
	 *
	 * @param s
	 * @param s1
	 * @return
	 */
	@Override
	public boolean restore(String s, String s1) {
		// // TODO:
		// String filePath = "/Users/vaishnavishah/Documents/COMPSCI578/PA3.2/fault-tolerant-db-gp/store.txt";

	    //     try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        //     StringBuilder content = new StringBuilder();
        //     String line;
		// 	System.out.println("restore");
        //     // Read lines from the file and append to the StringBuilder
        //     while ((line = reader.readLine()) != null) {
        //         content.append(line).append("\n");
        //     }

            // Print the read string
            //System.out.println("String read from the file:\n" + content.toString());
			// get string from file 
			String all_results = s1;//content.toString();
			Pattern pattern = Pattern.compile("Row\\[(-?\\d+), \\[([^\\]]+)\\]\\]");
			Matcher matcher = pattern.matcher(all_results);

			// Create a map to store the result
			Map<Integer, Vector<Integer>> resultMap = new HashMap<>();

			// Process each match
			while (matcher.find()) {
				int key = Integer.parseInt(matcher.group(1));
				String valuesString = matcher.group(2);

				// Split the values string and convert to Vector
				String[] valuesArray = valuesString.split(", ");
				Vector<Integer> valuesVector = new Vector<>();
				for (String value : valuesArray) {
					valuesVector.add(Integer.parseInt(value));
				}

				// Put the key-value pair in the map
				resultMap.put(key, valuesVector);
			}
			for (Map.Entry<Integer, Vector<Integer>> entry : resultMap.entrySet()) {
				int key = entry.getKey();
				Vector<Integer> values = entry.getValue();

				// Create the CQL insert statement
				String cql = String.format("INSERT INTO %s (id, events) VALUES (?, ?);", "grade");

				// Prepare the statement
				PreparedStatement preparedStatement = session.prepare(cql);

				// Execute the statement with the values
				session.execute(preparedStatement.bind(key, values));
			}
		//System.out.println("restore s "+ s + " s1 "+s1);
		
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
		return true;

	}


	/**
	 * No request types other than {@link edu.umass.cs.gigapaxos.paxospackets
	 * .RequestPacket will be used by Grader, so you don't need to implement
	 * this method.}
	 *
	 * @param s
	 * @return
	 * @throws RequestParseException
	 */
	@Override
	public Request getRequest(String s) throws RequestParseException {
		return null;
	}

	/**
	 * @return Return all integer packet types used by this application. For an
	 * example of how to define your own IntegerPacketType enum, refer {@link
	 * edu.umass.cs.reconfiguration.examples.AppRequest}. This method does not
	 * need to be implemented because the assignment Grader will only use
	 * {@link
	 * edu.umass.cs.gigapaxos.paxospackets.RequestPacket} packets.
	 */
	@Override
	public Set<IntegerPacketType> getRequestTypes() {
		return new HashSet<IntegerPacketType>();
	}
}
