import org.junit.Test;
import org.opencron.common.ext.ExtensionLoader;
import org.opencron.rpc.Client;
import org.opencron.rpc.Server;

import java.io.IOException;

public class SPITest {

    @Test
    public void testSpi() throws IOException {
        Server server = ExtensionLoader.load(Server.class);
        System.out.println(server.getClass());

        Client client = ExtensionLoader.load(Client.class);
        System.out.println(client.getClass());
    }

}
