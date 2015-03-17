package reactor.io.net.udp;

import io.netty.util.NetUtil;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.Environment;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 */
public class UdpServerTests {

	final Logger log = LoggerFactory.getLogger(getClass());

	Environment     env;
	ExecutorService threadPool;

	@Before
	public void setup() {
		env = Environment.initializeIfEmpty().assignErrorJournal();
		threadPool = Executors.newCachedThreadPool();
	}

	@After
	public void cleanup() throws InterruptedException {
		threadPool.shutdown();
		threadPool.awaitTermination(5, TimeUnit.SECONDS);
	}

	//@Test
	/*@Ignore
	public void supportsReceivingDatagrams() throws InterruptedException {
		final int port = SocketUtils.findAvailableUdpPort();
		final CountDownLatch latch = new CountDownLatch(4);

		final DatagramServer<byte[], byte[]> server = NetStreams.udpServer(s ->	s
								.env(env)
								.listen(port)
								.codec(StandardCodecs.BYTE_ARRAY_CODEC)
		);

		server.consume(ch -> ch.consume(new Consumer<byte[]>() {
			@Override
			public void accept(byte[] bytes) {
				if (bytes.length == 1024) {
					latch.countDown();
				}
			}
		}));

		server.start().onComplete(p -> {
			try {
				DatagramChannel udp = DatagramChannel.open();
				udp.configureBlocking(true);
				udp.connect(new InetSocketAddress(InetAddress.getLocalHost(), port));

				byte[] data = new byte[1024];
				new Random().nextBytes(data);
				for (int i = 0; i < 4; i++) {
					udp.write(ByteBuffer.wrap(data));
				}

				udp.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		assertThat("latch was counted down", latch.await(30, TimeUnit.SECONDS));
	}

	//@Test
	@SuppressWarnings("unchecked")
	@Ignore
	public void supportsUdpMulticast() throws InterruptedException,
			UnknownHostException,
			SocketException,
			TimeoutException,
			ExecutionException {
		final int port = SocketUtils.findAvailableUdpPort();
		final CountDownLatch latch = new CountDownLatch(Environment.PROCESSORS ^ 2);

		final InetAddress multicastGroup = InetAddress.getByName("230.0.0.1");
		final NetworkInterface multicastIface = findMulticastInterface();
		final Collection<DatagramServer<byte[], byte[]>> servers = new ArrayList<>();

		for (int i = 0; i < Environment.PROCESSORS; i++) {
			DatagramServer<byte[], byte[]> server = NetStreams.<byte[], byte[]>udpServer(NettyDatagramServer.class, spec -> spec
					.env(env)
					.dispatcher(Environment.SHARED)
					.listen(port)
					.multicastInterface(multicastIface)
					.options(new ServerSocketOptions()
							.reuseAddr(true)
							.protocolFamily(StandardProtocolFamily.INET))
					.codec(StandardCodecs.BYTE_ARRAY_CODEC)
			);

			server.consume(ch -> ch.consume(new Consumer<byte[]>() {
				int count = 0;

				@Override
				public void accept(byte[] bytes) {
					//log.info("{} got {} bytes", ++count, bytes.length);
					if (bytes.length == 1024) {
						latch.countDown();
					}
				}
			}));

			server.start().onSuccess(b -> server.join(multicastGroup, multicastIface)).await();

			servers.add(server);
		}

		for (int i = 0; i < Environment.PROCESSORS; i++) {
			threadPool.submit(new Runnable() {
				@Override
				public void run() {
					try {
						MulticastSocket multicast = new MulticastSocket(port);
						multicast.joinGroup(multicastGroup);

						byte[] data = new byte[1024];
						new Random().nextBytes(data);

						multicast.send(new DatagramPacket(data, data.length, multicastGroup, port));

						multicast.close();
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}
			}).get(5, TimeUnit.SECONDS);
		}

		assertThat("latch was counted down", latch.await(5, TimeUnit.SECONDS));

		for (DatagramServer s : servers) {
			s.shutdown().await();
		}
	}*/

	private NetworkInterface findMulticastInterface() throws SocketException {
		Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
		while (ifaces.hasMoreElements()) {
			NetworkInterface iface = ifaces.nextElement();
			if (!iface.isLoopback() && iface.supportsMulticast() && iface.isUp()) {
				return iface;
			}
		}
		return NetUtil.LOOPBACK_IF;
	}

}
