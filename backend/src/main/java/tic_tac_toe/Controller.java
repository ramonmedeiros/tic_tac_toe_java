package tic_tac_toe;

import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.json.simple.JSONObject;

@RestController
public class Controller {
	private HashMap<String, Game> games = new HashMap<String, Game>();
	private HashMap<String, String> users = new HashMap<String, String>();
	final String TITLE = "Tic Tac Toe by Ramon Medeiros";
	final String USERNAME = "username";
	final String TOKEN = "token";
	final String LINE = "line";
	final String COLUMN = "column";
	final String PLAYER = "player";
	private static final Logger LOGGER = Logger.getLogger(Game.class.getName());

	@CrossOrigin(origins = "*")
	@GetMapping("/")
	public String index() {
		return TITLE;
	}

	// handles errors on game
	@ExceptionHandler(GameErrorException.class)
	public ResponseEntity<String> handleError(HttpServletRequest req, Exception ex) {
		LOGGER.info("Request: " + req.getRequestURL() + " raised " + ex);
		JSONObject resp = new JSONObject();
		resp.put("message", ex.getMessage());
		return new ResponseEntity<>(resp.toJSONString(), HttpStatus.BAD_REQUEST);
	}
	
	// handles errors on game
	@ExceptionHandler(DrawException.class)
	public ResponseEntity<String> handleDraw(HttpServletRequest req, Exception ex) {
		LOGGER.info("Request: " + req.getRequestURL() + " raised " + ex);
		JSONObject resp = new JSONObject();
		resp.put("message", ex.getMessage());
		return new ResponseEntity<>(resp.toJSONString(), HttpStatus.ACCEPTED);
	}

	@ExceptionHandler(FinishException.class)
	public ResponseEntity<String> handleFinish(HttpServletRequest req, Exception ex) {
		LOGGER.info("Request: " + req.getRequestURL() + " raised " + ex);
		JSONObject resp = new JSONObject();
		resp.put("message", ex.getMessage());
		return new ResponseEntity<>(resp.toJSONString(), HttpStatus.ACCEPTED);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/token", params = { TOKEN, USERNAME }, produces = "application/json")
	public ResponseEntity<String> validateToken(@RequestParam(TOKEN) String token,
			@RequestParam(USERNAME) String username) {
		for (Entry<String, String> user1 : this.users.entrySet()) {
			if ((user1.getKey().compareTo(username) == 0) && (user1.getValue().compareTo(token) == 0)) {
				return new ResponseEntity<>("valid", HttpStatus.OK);
			}
		}
		return new ResponseEntity<>("invalid", HttpStatus.NOT_FOUND);
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/login", produces = "application/json")
	public String login(@RequestBody Map<String, String> user) {
		String username = user.get(USERNAME);
		String token = UUID.randomUUID().toString();
		this.users.put(username, token);
		JSONObject ret = new JSONObject();
		ret.put(TOKEN, token);
		return ret.toString();
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/logout", produces = "application/json")
	public ResponseEntity<String> logout(@RequestBody Map<String, String> userToken) {
		String username = userToken.get(USERNAME);
		String token = userToken.get(TOKEN);
		if (this.users.get(username).compareTo(token) == 0) {
			this.users.remove(username);
			return new ResponseEntity<>("accepted", HttpStatus.OK);
		}
		return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/game")
	public ResponseEntity<String> gameGET() {
		JSONObject resp = new JSONObject();
		resp.put("games", new ArrayList(this.games.keySet()));
		return new ResponseEntity<>(resp.toJSONString(), HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/game", produces = "application/json")
	public ResponseEntity<String> gamePOST(@RequestBody Map<String, String> tokenP) {
		String token = tokenP.get(TOKEN);
		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}
		Game game = new Game();
		this.games.put(game.game_id, game);
		return new ResponseEntity<>(game.game_id, HttpStatus.CREATED);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/game/{uuid}", produces = "application/json")
	public ResponseEntity<String> getGameUuid(@PathVariable("uuid") String uuid) {
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
		}
		JSONObject resp = new JSONObject();
		resp.put("board", this.games.get(uuid).getListBoard());
		resp.put("winner", this.games.get(uuid).getWinner());
		resp.put("players", this.games.get(uuid).getPlayers());

		// String resp = new JSONObject(Map.of("board",
		// this.games.get(uuid).getListBoard(), "winner",
		// this.games.get(uuid).getWinner(), "players",
		// this.games.get(uuid).getPlayers())).toJSONString();
		return new ResponseEntity<>(resp.toJSONString(), HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/game/{uuid}", produces = "application/json")
	public ResponseEntity<String> postGameUuid(@PathVariable("uuid") String uuid,
			@RequestBody Map<String, String> params) {
		String token = params.get(TOKEN);
		String line = params.get(LINE);
		String column = params.get(COLUMN);
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
		}

		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}

		if (this.games.get(uuid).doMove(line, column, token) == true) {
			return new ResponseEntity<>("moved", HttpStatus.OK);
		}
		return new ResponseEntity<>("failed", HttpStatus.FORBIDDEN);
	}

	@CrossOrigin(origins = "*")
	@DeleteMapping(value = "/game/{uuid}", produces = "application/json")
	public ResponseEntity<String> deleteGameUuid(@PathVariable("uuid") String uuid,
			@RequestBody Map<String, String> params) {
		String token = params.get(TOKEN);
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
		}
		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}

		this.games.remove(uuid);
		return new ResponseEntity<>("deleted", HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@PostMapping(value = "/game/{uuid}/player", produces = "application/json")
	public ResponseEntity<String> addPlayer(@PathVariable("uuid") String uuid,
			@RequestBody Map<String, String> params) {
		String token = params.get(TOKEN);
		String player = params.get(PLAYER);
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
		}
		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}

		// check if player is already
		if (this.games.get(uuid).getPlayers().contains(player) == true) {
			return new ResponseEntity<>(String.format("Player %s its already being used", player),
					HttpStatus.BAD_REQUEST);
		}

		this.games.get(uuid).players.put(player, token);
		return new ResponseEntity<>("done", HttpStatus.OK);
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/game/{uuid}/player", params = { TOKEN }, produces = "application/json")
	public ResponseEntity<String> getPlayers(@PathVariable("uuid") String uuid, @RequestParam(TOKEN) String token) {
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
		}
		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}

		String player = this.games.get(uuid).getPlayerByToken(token);

		if (player == null) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
		}

		JSONObject resp = new JSONObject();
		resp.put("player", player);
		return new ResponseEntity<>(resp.toJSONString(), HttpStatus.OK);
	}

}
