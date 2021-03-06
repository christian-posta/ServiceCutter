package ch.hsr.servicecutter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import ch.hsr.servicecutter.model.EngineState;
import ch.hsr.servicecutter.model.usersystem.Nanoentity;
import ch.hsr.servicecutter.model.usersystem.UserSystem;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = EngineServiceAppication.class)
@IntegrationTest("server.port=0")
@WebAppConfiguration
public class EngineServiceRestTest {

	@Value("${local.server.port}")
	private int port;
	private RestTemplate restTemplate = new TestRestTemplate();

	@Test
	public void statusCheck() {
		ResponseEntity<EngineState> entity = this.restTemplate.getForEntity("http://localhost:" + this.port + "/engine", EngineState.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("Engine is up and running.", entity.getBody().getDescription());
	}

	@Test
	public void createEmptyModel() {
		String name = "testModel";
		HttpEntity<String> request = IntegrationTestHelper.createHttpRequestWithPostObj(name);
		ResponseEntity<UserSystem> requestResult = this.restTemplate.exchange("http://localhost:" + this.port + "/engine/systems", HttpMethod.POST, request, UserSystem.class);
		assertEquals(HttpStatus.OK, requestResult.getStatusCode());
		Long id = requestResult.getBody().getId();

		ResponseEntity<UserSystem> assertResult = getSystem(id);
		assertEquals("testModel", assertResult.getBody().getName().toString());
	}

	@Test
	public void createAndDeleteSystem() {
		String name = "testModel";
		HttpEntity<String> request = IntegrationTestHelper.createHttpRequestWithPostObj(name);
		ResponseEntity<UserSystem> requestResult = this.restTemplate.exchange("http://localhost:" + this.port + "/engine/systems", HttpMethod.POST, request, UserSystem.class);
		assertEquals(HttpStatus.OK, requestResult.getStatusCode());
		Long id = requestResult.getBody().getId();

		ResponseEntity<UserSystem> assertResult = getSystem(id);
		assertEquals("testModel", assertResult.getBody().getName());

		this.restTemplate.delete(createURL(id));
		ResponseEntity<?> notFoundResult = this.restTemplate.exchange(createURL(id), HttpMethod.GET, IntegrationTestHelper.createEmptyHttpRequest(), (Class<?>) null);
		// TODO it would be more appropriate to receive a 404 here
		assertThat(notFoundResult.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
	}

	private ResponseEntity<UserSystem> getSystem(Long id) {
		return this.restTemplate.exchange(createURL(id), HttpMethod.GET, IntegrationTestHelper.createEmptyHttpRequest(), UserSystem.class);
	}

	private String createURL(Long id) {
		return "http://localhost:" + this.port + "/engine/systems/" + id;
	}

	@Test
	public void createModelWithEntities() {
		int before = countSystems();
		createModelOnApi();
		// test whether created model is visible
		assertEquals(before + 1, countSystems());
	}

	private Long createModelOnApi() {
		UserSystem model = createSystem();
		HttpEntity<UserSystem> request = IntegrationTestHelper.createHttpRequestWithPostObj(model);
		ResponseEntity<UserSystem> entity = restTemplate.exchange("http://localhost:" + this.port + "/engine/systems/", HttpMethod.POST, request, UserSystem.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		return entity.getBody().getId();
	}

	private int countSystems() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> response = restTemplate.getForEntity("http://localhost:" + this.port + "/engine/systems", List.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		int systems = response.getBody().size();
		return systems;
	}

	private UserSystem createSystem() {
		UserSystem userSystem = new UserSystem();
		userSystem.setName("firstModel");
		Nanoentity nanoentity1 = new Nanoentity();
		nanoentity1.setName("firstNanoentity");
		Nanoentity nanoentity2 = new Nanoentity();
		nanoentity2.setName("secondNanoentity");
		userSystem.addNanoentity(nanoentity1);
		userSystem.addNanoentity(nanoentity2);
		return userSystem;
	}

}
