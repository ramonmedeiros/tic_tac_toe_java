package tic_tac_toe;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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

	@Before
	public void setUp() throws Exception {
		// login user X
		String userXJson = new JSONObject(Map.of(USERNAME, X_USERNAME)).toJSONString();
		String userOJson = new JSONObject(Map.of(USERNAME, O_USERNAME)).toJSONString();
		MvcResult Xresponse = mvc
				.perform(MockMvcRequestBuilders.post("/login").contentType(MediaType.APPLICATION_JSON)
						.content(userXJson))
				.andExpect(status().isOk()).andExpect(content().string(containsString("token"))).andReturn();
		JSONObject XToken = (JSONObject) jsonparser.parse(Xresponse.getResponse().getContentAsString());
		this.X_token = (String) XToken.get(TOKEN);

		// login user O
		MvcResult Oresponse = mvc
				.perform(MockMvcRequestBuilders.post("/login").contentType(MediaType.APPLICATION_JSON)
						.content(userOJson))
				.andExpect(status().isOk()).andExpect(content().string(containsString("token"))).andReturn();
		JSONObject OToken = (JSONObject) jsonparser.parse(Oresponse.getResponse().getContentAsString());
		this.O_token = (String) OToken.get(TOKEN);

		// start game
		MvcResult newGame = mvc
				.perform(MockMvcRequestBuilders.post("/game").contentType(MediaType.APPLICATION_JSON)
						.content(new JSONObject(Map.of(TOKEN, this.X_token)).toJSONString()))
				.andExpect(status().isCreated()).andReturn();
		this.gameUUID = newGame.getResponse().getContentAsString();
		this.gameURI = String.format("/game/%s", this.gameUUID);
		
		// when not register, player should return 404
		String gameplayeruri = String.format("/game/%s/player", this.gameUUID);
		mvc.perform(MockMvcRequestBuilders.get(gameplayeruri).contentType(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isNotFound());
		
		// register player X
		String xparams = new JSONObject(Map.of(PLAYER, "X", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(gameplayeruri).contentType(MediaType.APPLICATION_JSON).content(xparams))
				.andExpect(status().isOk());

		// register player O
		String oparams = new JSONObject(Map.of(PLAYER, "O", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(gameplayeruri).contentType(MediaType.APPLICATION_JSON).content(oparams))
				.andExpect(status().isOk());
	}

	@After
	public void logout() throws Exception {
		// logout user X
		String userXlogout = new JSONObject(Map.of(USERNAME, X_USERNAME, TOKEN, this.X_token)).toJSONString();
		String userOlogout = new JSONObject(Map.of(USERNAME, O_USERNAME, TOKEN, this.O_token)).toJSONString();

		mvc.perform(MockMvcRequestBuilders.post("/logout").contentType(MediaType.APPLICATION_JSON).content(userXlogout))
				.andExpect(status().isOk());

		// logout user O
		mvc.perform(MockMvcRequestBuilders.post("/logout").contentType(MediaType.APPLICATION_JSON).content(userOlogout))
				.andExpect(status().isOk());
	}

	@Test
	public void getHello() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().string(equalTo("Tic Tac Toe by Ramon Medeiros")));
	}

	@Test
	public void getGames() throws Exception {
		MvcResult game = mvc.perform(MockMvcRequestBuilders.get("/game").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andReturn();
		JSONObject games = (JSONObject) jsonparser.parse(game.getResponse().getContentAsString());
		List<String> gamesList = (List) games.get("games");
		assertTrue(gamesList.contains(this.gameUUID));
	}

	@Test
	public void validateXToken() throws Exception {
		// validate user X
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add(TOKEN, this.X_token);
		params.add(USERNAME, this.X_USERNAME);
		mvc.perform(MockMvcRequestBuilders.get("/token").contentType(MediaType.APPLICATION_JSON).params(params))
				.andExpect(status().isOk());
	}

	@Test
	public void invalidToken() throws Exception {
		// validate user X
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add(TOKEN, "invalid-token");
		params.add(USERNAME, this.X_USERNAME);
		mvc.perform(MockMvcRequestBuilders.get("/token").contentType(MediaType.APPLICATION_JSON).params(params))
				.andExpect(status().isNotFound());
	}	
	
	@Test
	public void getPlayerByToken() throws Exception {
		String gameplayeruri = String.format("%s/player", this.gameURI);
		MvcResult newGame = mvc.perform(
				MockMvcRequestBuilders.get(gameplayeruri).accept(MediaType.APPLICATION_JSON).param(TOKEN, this.X_token))
				.andExpect(status().isOk()).andReturn();
		JSONObject player = (JSONObject) jsonparser.parse(newGame.getResponse().getContentAsString());
		assertEquals(player.get(PLAYER), "X");
	}

	@Test
	public void getNonExistingGame() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/game/non-exist").contentType(MediaType.APPLICATION_JSON)
				.content(new JSONObject(Map.of(TOKEN, this.X_token)).toJSONString()))
				.andExpect(status().is4xxClientError());
	}

	@Test
	public void getAvailablePlayers() throws Exception {
		MvcResult game = mvc.perform(MockMvcRequestBuilders.get(this.gameURI)).andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(game.getResponse().getContentAsString());
		List<String> players = (JSONArray) response.get("players");
		assertEquals(players.get(0), "X");
	}

	@Test
	public void doMoveLine() throws Exception {
		String xparams = new JSONObject(Map.of(COLUMN, "0", LINE, "0", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(xparams))
				.andExpect(status().isOk());

		MvcResult game = mvc
				.perform(MockMvcRequestBuilders.get(this.gameURI).contentType(MediaType.APPLICATION_JSON)
						.content(new JSONObject(Map.of(TOKEN, this.X_token)).toJSONString()))
				.andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(game.getResponse().getContentAsString());
		List<List<String>> board = (JSONArray) response.get("board");
		assertEquals(board.get(0).get(0), "X");
	}

	@Test
	public void doMoveSamePlace() throws Exception {
		// do first move
		String xparams = new JSONObject(Map.of(COLUMN, "0", LINE, "0", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(xparams))
				.andExpect(status().isOk());

		// assert first move
		MvcResult firstMove = mvc
				.perform(MockMvcRequestBuilders.get(this.gameURI).contentType(MediaType.APPLICATION_JSON)
						.content(new JSONObject(Map.of(TOKEN, this.X_token)).toJSONString()))
				.andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(firstMove.getResponse().getContentAsString());
		List<List<String>> board = (JSONArray) response.get("board");
		assertEquals(board.get(0).get(0), "X");

		// try to do same move and expect error
		String oparams = new JSONObject(Map.of(COLUMN, "0", LINE, "0", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(oparams))
				.andExpect(status().isBadRequest());

	}

	@Test
	public void doMoveColumn() throws Exception {
		String xparams = new JSONObject(Map.of(COLUMN, "1", LINE, "0", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(xparams))
				.andExpect(status().isOk());

		MvcResult game = mvc.perform(MockMvcRequestBuilders.get(this.gameURI).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andReturn();
		JSONObject response = (JSONObject) jsonparser.parse(game.getResponse().getContentAsString());
		List<List<String>> board = (JSONArray) response.get("board");
		assertEquals(board.get(0).get(1), "X");
	}

	@Test
	public void doWinGame() throws Exception {
		// board[0][0] == O
		String b00 = new JSONObject(Map.of(COLUMN, "0", LINE, "0", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b00))
				.andExpect(status().isOk());

		// board[1][1] == X
		String b11 = new JSONObject(Map.of(COLUMN, "1", LINE, "1", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b11))
				.andExpect(status().isOk());

		// board[0][1] == O
		String b01 = new JSONObject(Map.of(COLUMN, "0", LINE, "1", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b01))
				.andExpect(status().isOk());

		// board[1][2] == X
		String b12 = new JSONObject(Map.of(COLUMN, "1", LINE, "2", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b12))
				.andExpect(status().isOk());

		// board[0][2] == O
		String b02 = new JSONObject(Map.of(COLUMN, "0", LINE, "2", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b02))
				.andExpect(status().isAccepted());

	}
	
	@Test
	public void drawGame() throws Exception {

		// board[0][2] == O
		String b02 = new JSONObject(Map.of(COLUMN, "2", LINE, "0", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b02))
				.andExpect(status().isOk());

		// board[0][0] == X
		String b00 = new JSONObject(Map.of(COLUMN, "0", LINE, "0", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b00))
				.andExpect(status().isOk());

		// board[0][1] == O
		String b01 = new JSONObject(Map.of(COLUMN, "1", LINE, "0", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b01))
				.andExpect(status().isOk());

		// board[1][1] == X
		String b11 = new JSONObject(Map.of(COLUMN, "1", LINE, "1", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b11))
				.andExpect(status().isOk());

		// board[1][0] == O
		String b10 = new JSONObject(Map.of(COLUMN, "0", LINE, "1", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b10))
				.andExpect(status().isOk());

		// board[1][2] == X
		String b12 = new JSONObject(Map.of(COLUMN, "2", LINE, "1", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b12))
				.andExpect(status().isOk());

		// board[2][0] == O
		String b20 = new JSONObject(Map.of(COLUMN, "0", LINE, "2", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b20))
						.andExpect(status().isOk());

		// board[2][1] == X
		String b21 = new JSONObject(Map.of(COLUMN, "1", LINE, "2", TOKEN, this.X_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b21))
				.andExpect(status().isOk());
		
		// board[2][2] == O
		String b22 = new JSONObject(Map.of(COLUMN, "2", LINE, "2", TOKEN, this.O_token)).toJSONString();
		mvc.perform(MockMvcRequestBuilders.post(this.gameURI).contentType(MediaType.APPLICATION_JSON).content(b22))
						.andExpect(status().isAccepted());
	}

}
