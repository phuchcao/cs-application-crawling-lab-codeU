package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Node;

import redis.clients.jedis.Jedis;


public class WikiCrawler {
	// keeps track of where we started
	private final String source;
	
	// the index where the results go
	private JedisIndex index;
	
	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();
	
	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b 
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
        if (testing){
        	System.out.println("in crawl, true");
        	System.out.println(queue.size());
        	String url = queue.poll();
        	System.out.println(url);
        	Elements content = wf.readWikipedia(url);
        	index.indexPage(url, content);
        	queueInternalLinks(content);
        	return url;
        } else {
        	System.out.println("in crawl, false");
        	String url = queue.poll();
        	if (index.isIndexed(url) == false){
        		Elements content = wf.fetchWikipedia(url);
        		index.indexPage(url, content);
        		queueInternalLinks(content);
        		return url;
        	} else {
        		return null;
        	}
        }
	}
	
	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
		System.out.println("in queue");
        for (int i = 0; i < paragraphs.size(); i++){
        	Element para = paragraphs.get(i);
        	Elements allLinks = para.getElementsByAttribute("href");
        	for (Element elem: allLinks){
        		//System.out.println(elem.attr("abs:href"));
        		//System.out.println(elem.attr("href"));
        		String newLink = elem.attr("href");
        		if (newLink.length() >= 5){
        			if (newLink.substring(0, 5).equals("/wiki")){
        				String absLink = "https://en.wikipedia.org" + newLink;
        				queue.offer(absLink);
        				//System.out.println(queue.size() + " " + absLink);
        			}
        		}
        	}
        }
	}

	public static void main(String[] args) throws IOException {
		
		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);

            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
            //break;
		} while (res == null);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}
}
