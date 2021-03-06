/*
 * ListToMapTest.java
 *
 * Created on 2018-03-23, 6:57
 */
package com.marcnuri.demo.listtomap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2018-03-23.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ListToMapTest.ListToMapConfiguration.class})
public class ListToMapTest {

//**************************************************************************************************
//  Fields
//**************************************************************************************************
	private static final String CURRENT_REPO_NAME = "java8-streams-list-to-map";

	@Autowired
	private RestTemplate restTemplate;

//**************************************************************************************************
//  Constructors
//**************************************************************************************************

//**************************************************************************************************
//  Abstract Methods
//**************************************************************************************************

//**************************************************************************************************
//  Overridden Methods
//**************************************************************************************************

//**************************************************************************************************
//  Other Methods
//**************************************************************************************************
	@Test
	public void listToMap_noDuplicatesList_shouldReturnOk() {
		// Given
		final String marcnuriDemoReposUrl = "https://api.github.com/orgs/marcnuri-demo/repos";
		final List<GithubRepo> repos = restTemplate.exchange( marcnuriDemoReposUrl, HttpMethod.GET,
				new HttpEntity<>(new HttpHeaders()), new ParameterizedTypeReference<List<GithubRepo>>(){}).getBody();

		// When
		final Map<String, GithubRepo> repoMap = repos.stream().collect(Collectors.toMap(GithubRepo::getName, Function.identity()));

		// Then
		assertFalse(repoMap.isEmpty());
		assertNotNull(repoMap.get(CURRENT_REPO_NAME));
	}

	@Test(expected = IllegalStateException.class)
	public void listToMap_duplicatesList_shouldThrowException() {
		// Given
		final String marcnuriDemoReposUrl = "https://api.github.com/orgs/marcnuri-demo/repos";
		final List<GithubRepo> repos = new ArrayList<>();
		for(int it = 0; it < 2; it++) {
			repos.addAll(restTemplate.exchange( marcnuriDemoReposUrl, HttpMethod.GET,
					new HttpEntity<>(new HttpHeaders()), new ParameterizedTypeReference<List<GithubRepo>>(){}).getBody());
		}

		// When
		final Map<String, GithubRepo> repoMap = repos.stream().collect(Collectors.toMap(GithubRepo::getName, Function.identity()));

		// Then
		fail();
	}

	@Test
	public void listToMapHandleDuplicates_duplicatesList_shouldReturnOk() {
		// Given
		final String marcnuriDemoReposUrl = "https://api.github.com/orgs/marcnuri-demo/repos";
		final List<GithubRepo> repos = new ArrayList<>();
		for(int it = 0; it < 2; it++) {
			final List<GithubRepo> temp = restTemplate.exchange( marcnuriDemoReposUrl, HttpMethod.GET,
					new HttpEntity<>(new HttpHeaders()), new ParameterizedTypeReference<List<GithubRepo>>(){}).getBody();
			for(GithubRepo ghr : temp) {
				 ghr.setLocalVersion(it);
			}
			repos.addAll(temp);
		}

		// When
		final Map<String, GithubRepo> repoMap = repos.stream().collect(Collectors.toMap(GithubRepo::getName, Function.identity(),
				(ghrPrevious, ghrNew) -> ghrNew));

		// Then
		final int newestVersion = 1;
		assertFalse(repoMap.isEmpty());
		final GithubRepo currentRepo = repoMap.get(CURRENT_REPO_NAME);
		assertNotNull(currentRepo);
		assertEquals(newestVersion, currentRepo.getLocalVersion().intValue());
	}

	@Test
	public void listToMapSorted_unsortedList_shouldReturnNaturalSortedMap() {
		// Given
		final String marcnuriDemoReposUrl = "https://api.github.com/orgs/marcnuri-demo/repos";
		final List<GithubRepo> repos = restTemplate.exchange( marcnuriDemoReposUrl, HttpMethod.GET,
				new HttpEntity<>(new HttpHeaders()), new ParameterizedTypeReference<List<GithubRepo>>(){}).getBody();

		// When
		final SortedMap<String, GithubRepo> repoMap = repos.stream().collect(Collectors.toMap(GithubRepo::getName, Function.identity(),
				(ghrPrevious, ghrNew) -> ghrNew, TreeMap::new));

		// Then
		assertFalse(repoMap.isEmpty());
		assertNotNull(repoMap.get(CURRENT_REPO_NAME));
		assertTrue(isSorted(repoMap.keySet(), Comparator.naturalOrder()));
	}

	@Test
	public void listToMapReverseSorted_unsortedList_shouldReturnReverseSortedMap() {
		// Given
		final String marcnuriDemoReposUrl = "https://api.github.com/orgs/marcnuri-demo/repos";
		final List<GithubRepo> repos = restTemplate.exchange( marcnuriDemoReposUrl, HttpMethod.GET,
				new HttpEntity<>(new HttpHeaders()), new ParameterizedTypeReference<List<GithubRepo>>(){}).getBody();

		// When
		final SortedMap<String, GithubRepo> repoMap = repos.stream().collect(Collectors.toMap(GithubRepo::getName, Function.identity(),
				(ghrPrevious, ghrNew) -> ghrNew,
				() -> new TreeMap<>(Comparator.reverseOrder())));

		// Then
		assertFalse(repoMap.isEmpty());
		assertNotNull(repoMap.get(CURRENT_REPO_NAME));
		assertTrue(isSorted(repoMap.keySet(), Comparator.reverseOrder()));
	}
//**************************************************************************************************
//  Getter/Setter Methods
//**************************************************************************************************

//**************************************************************************************************
//  Static Methods
//**************************************************************************************************
	private static <T extends Comparable> boolean isSorted(Collection<T> list, Comparator<T> comparator) {
		boolean ret = true;
		T previous = null;
		for (T t: list) {
			if (previous != null && comparator.compare(previous, t) > 0) {
				ret = false;
				break;
			}
			previous = t;
		}
		return ret;
	}

//**************************************************************************************************
//  Inner Classes
//**************************************************************************************************
	@Configuration
	protected static class ListToMapConfiguration {

		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}

	}
}
