package tic_tac_toe;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = tic_tac_toe.Application.class)
@AutoConfigureMockMvc
public class ControllerTest {
	String X_token, O_token, gameUUID, gameURI;
	final String X_USERNAME = "X-name";
	final String O_USERNAME = "O-name";
	final String USERNAME = "username";
	final String TOKEN = "token";
	final String PLAYER = "player";
	final String COLUMN = "column";
	final String LINE = "line";
	JSONParser jsonparser = new JSONParser();

	@Autowired
	private MockMvc mvc;

	@Test
	public void getHello() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().string(equalTo("Tic Tac Toe by Ramon Medeiros")));
	}

	@Before
	public void setUp() throws Exception {
		// login user X
		MvcResult Xresponse = mvc
				.perform(MockMvcRequestBuilders.post("/login").accept(MediaType.APPLICATION_JSON).param(USERNAME,
						X_USERNAME))
				.andExpect(status().isOk()).andExpect(content().string(containsString("token"))).andReturn();
		JSONObject XToken = (JSONObject) jsonparser.parse(Xresponse.getResponse().getContentAsString());
		this.X_token = (String) XToken.get(TOKEN);

		// login user O
		MvcResult Oresponse = mvc
				.perform(MockMvcRequestBuilders.post("/login").accept(MediaType.APPLICATION_JSON).param(USERNAME,
						O_USERNAME))
				.andExpect(status().isOk()).andExpect(content().string(containsString("token"))).andReturn();
		JSONObject OToken = (JSONObject) jsonparser.parse(Oresponse.getResponse().getContentAsString());
		this.O_token = (String) OToken.get(TOKEN);

		// start game
		MvcResult newGame = mvc.perform(
				MockMvcRequestBuilders.post("/game").accept(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isOk()).andReturn();
		this.gameUUID = newGame.getResponse().getContentAsString();
		this.gameURI = String.format("/game/%s", this.gameUUID);

		// register player X
		String gameplayeruri = String.format("/game/%s/player", this.gameUUID);
		MultiValueMap<String, String> xparams = new LinkedMultiValueMap<>();
		xparams.add(PLAYER, "X");
		xparams.add(TOKEN, this.X_token);
		mvc.perform(MockMvcRequestBuilders.post(gameplayeruri).accept(MediaType.APPLICATION_JSON).params(xparams))
				.andExpect(status().isOk());

		// register player O
		MultiValueMap<String, String> oparams = new LinkedMultiValueMap<>();
		oparams.add(PLAYER, "O");
		oparams.add(TOKEN, this.O_token);
		mvc.perform(MockMvcRequestBuilders.post(gameplayeruri).accept(MediaType.APPLICATION_JSON).params(oparams))
				.andExpect(status().isOk());
	}

	@After
	public void logout() throws Exception {
		// logout user X
		MultiValueMap<String, String> xparams = new LinkedMultiValueMap<>();
		xparams.add(USERNAME, X_USERNAME);
		xparams.add(TOKEN, this.X_token);
		mvc.perform(MockMvcRequestBuilders.post("/logout").accept(MediaType.APPLICATION_JSON).params(xparams))
				.andExpect(status().isOk());

		// logout user O
		MultiValueMap<String, String> oparams = new LinkedMultiValueMap<>();
		oparams.add(USERNAME, O_USERNAME);
		oparams.add(TOKEN, this.O_token);
		mvc.perform(MockMvcRequestBuilders.post("/logout").accept(MediaType.APPLICATION_JSON).params(oparams))
				.andExpect(status().isOk());
	}

	@Test
	public void getPlayerByToken() throws Exception {
		String gameplayeruri = String.format("%s/player", this.gameURI);
		MvcResult newGame = mvc.perform(
				MockMvcRequestBuilders.get(gameplayeruri).accept(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isOk()).andReturn();
		assertEquals(newGame.getResponse().getContentAsString(), "X");
	}

	@Test
	public void getNonExistingGame() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/game/non-exist").accept(MediaType.APPLICATION_JSON).param(TOKEN,
				this.X_token)).andExpect(status().is4xxClientError());
	}

	@Test
	public void getAvailablePlayers() throws Exception {
		MvcResult game = mvc.perform(
				MockMvcRequestBuilders.get(this.gameURI).accept(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(game.getResponse().getContentAsString());
		List<String> players = (JSONArray) response.get("players");
		assertEquals(players.get(0), "X");
	}

	@Test
	public void doMoveLine() throws Exception {
		MultiValueMap<String, String> xparams = new LinkedMultiValueMap<>();
		xparams.add(COLUMN, "0");
		xparams.add(LINE, "0");
		xparams.add(TOKEN, this.X_token);
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).accept(MediaType.APPLICATION_JSON).params(xparams))
				.andExpect(status().isOk());

		MvcResult game = mvc.perform(
				MockMvcRequestBuilders.get(this.gameURI).accept(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(game.getResponse().getContentAsString());
		List<List<String>> board = (JSONArray) response.get("board");
		assertEquals(board.get(0).get(0), "X");
	}

	@Test
	public void doMoveSamePlace() throws Exception {
		// do first move
		MultiValueMap<String, String> xparams = new LinkedMultiValueMap<>();
		xparams.add(COLUMN, "0");
		xparams.add(LINE, "0");
		xparams.add(TOKEN, this.X_token);
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).accept(MediaType.APPLICATION_JSON).params(xparams))
				.andExpect(status().isOk());

		// assert first move
		MvcResult firstMove = mvc.perform(
				MockMvcRequestBuilders.get(this.gameURI).accept(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(firstMove.getResponse().getContentAsString());
		List<List<String>> board = (JSONArray) response.get("board");
		assertEquals(board.get(0).get(0), "X");

		// try to do same move and expect error
		MultiValueMap<String, String> oparams = new LinkedMultiValueMap<>();
		oparams.add(COLUMN, "0");
		oparams.add(LINE, "0");
		oparams.add(TOKEN, this.O_token);
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).accept(MediaType.APPLICATION_JSON).params(oparams))
				.andExpect(status().isForbidden());

	}
	
	@Test
	public void doMoveColumn() throws Exception {
		MultiValueMap<String, String> xparams = new LinkedMultiValueMap<>();
		xparams.add(COLUMN, "1");
		xparams.add(LINE, "0");
		xparams.add(TOKEN, this.X_token);
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).accept(MediaType.APPLICATION_JSON).params(xparams))
				.andExpect(status().isOk());

		MvcResult game = mvc.perform(
				MockMvcRequestBuilders.get(this.gameURI).accept(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(game.getResponse().getContentAsString());
		List<List<String>> board = (JSONArray) response.get("board");
		assertEquals(board.get(0).get(1), "X");
	}

}
