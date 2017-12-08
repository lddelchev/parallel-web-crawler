package webcrawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Represents a web crawler that starts from a given web page and recursively
 * searches for a given content in it and its subpages. The searching is
 * performed on multiple threads.
 */
public class ParallelWebCrawler extends RecursiveTask<List<URI>> {
    private final Set<URI> visitedUrls;
    private final URI currentLocation;
    private final String needle;

    /**
     *
     * @param currentLocation the starting URI of the crawling
     * @param needle the searched content
     * @param visitedUrls the URLs that are already visited
     */
    public ParallelWebCrawler(
	        final URI currentLocation,
            final String needle,
            final Set<URI> visitedUrls) {
	    this.currentLocation = currentLocation;
	    this.needle = needle;
	    this.visitedUrls = visitedUrls;
    }

    /**
     *
     * @return list of URIs where the searched content is found by recursive
     * crawling of the subtree.
     */
	public List<URI> compute() {
	    List<URI> result = new ArrayList<>();

            String urlContents = getContents(currentLocation);
            visitedUrls.add(currentLocation);

            if (urlContents.contains(needle)) {
                System.out.println("Result found");
                result.add(currentLocation);
            } else {
                List<ParallelWebCrawler> subCrawlers = new ArrayList<>();
                List<URI> relevantURIs = getAllRelevantURIs(urlContents);
                for (URI uri : relevantURIs) {
                    if (!visitedUrls.contains(uri)) {
                        ParallelWebCrawler subCrawler =
                                new ParallelWebCrawler(uri, needle, visitedUrls);
                        subCrawlers.add(subCrawler);
                        subCrawler.fork();
                    }
                }

                for (ParallelWebCrawler subCrawler : subCrawlers) {
                    result.addAll(subCrawler.join());
                }
            }

        return result;
	}

	private boolean isInsideDomain(final URI asUrl)
            throws URISyntaxException {
		return currentLocation.getHost().equals(asUrl.getHost());
	}

	private URI normalizeLink(final String link)
            throws MalformedURLException, URISyntaxException {
		URI uri = new URI(link);
		if (uri.getScheme() != null && uri.getHost() != null) {
			return uri;
		}
		return URIUtils.resolve(currentLocation, uri);
	}

	private String getContents(final URI startLocation) {
		HttpClient httpClient = new DefaultHttpClient();
		System.out.println("Currently crawling : " + startLocation);
		HttpGet get = new HttpGet(startLocation);
		try {
			HttpResponse response = httpClient.execute(get);
			String contents =
                    IOUtils.toString(response.getEntity().getContent());
			return contents;
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private List<URI> getAllRelevantURIs(final String content) {
		ArrayList<URI> resultList = new ArrayList<>();
		String regex = "<a.*?href=\"((?!javascript).*?)\".*?>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
		    try {
                String link = matcher.group(1);
                final URI asUri = normalizeLink(link);
                if (isInsideDomain(asUri)) {
                    resultList.add(asUri);
                }
            } catch (MalformedURLException | URISyntaxException e) { }
		}
		return resultList;
	}
}
