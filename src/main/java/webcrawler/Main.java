package webcrawler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {
	public static void main(String[] args)
			throws IOException, URISyntaxException, InterruptedException, ExecutionException {
		URI startLocation = new URI("https://www.9gag.com/");
		ParallelWebCrawler crawler =
				new ParallelWebCrawler(startLocation, "cool",
						Collections.synchronizedSet(new HashSet<URI>()));
		List<URI> resultPage = crawler.compute();
		System.out.println("result :" + resultPage.toString());
	}

}
