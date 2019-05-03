package tic_tac_toe;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.json.simple.JSONObject;;

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

	@RequestMapping("/")
	public String index() {
		return TITLE;
	}

	// Total control - setup a model and return the view name yourself. Or
	// consider subclassing ExceptionHandlerExceptionResolver (see below).
	@ExceptionHandler(Exception.class)
	public ModelAndView handleError(HttpServletRequest req, Exception ex) {
		LOGGER.info("Request: " + req.getRequestURL() + " raised " + ex);

		ModelAndView mav = new ModelAndView();
		mav.addObject("exception", ex);
		mav.setViewName("error");
		return mav;
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST, params = { USERNAME }, produces = "application/json")
	public String login(@RequestParam(USERNAME) String username) {
		String token = UUID.randomUUID().toString();
		this.users.put(username, token);
		JSONObject ret = new JSONObject();
		ret.put(TOKEN, token);
		return ret.toString();
	}

	@RequestMapping(value = "/logout", method = RequestMethod.POST, params = { USERNAME,
			TOKEN }, produces = "application/json")
	public ResponseEntity<String> logout(@RequestParam(USERNAME) String username, @RequestParam(TOKEN) String token) {
		if (this.users.get(username).compareTo(token) == 0) {
			this.users.remove(username);
			return new ResponseEntity<>("accepted", HttpStatus.OK);
		}
		return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
	}

	@RequestMapping(value = "/game", method = RequestMethod.GET)
	public String gameGET() {
		return this.games.keySet().toString();
	}

	@RequestMapping(value = "/game", method=RequestMethod.POST,params = {TOKEN}, produces = "application/json")
	public ResponseEntity<String> gamePOST(@RequestParam(TOKEN) String token) {
		  if (this.users.containsValue(token) == false) {
			  return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		  }
		  Game game = new Game();
		  this.games.put(game.game_id, game);
		  return new ResponseEntity<>(game.game_id, HttpStatus.OK); 
	}

	@RequestMapping(value = "/game/**", method=RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getGameUuid(@PathVariable("uuid") String uuid) {
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND); 
		}
		
		JSONObject resp = new JSONObject();
		resp.put("board", this.games.get(uuid).getBoard());
		resp.put("winner", this.games.get(uuid).getWinner());
		resp.put("players", this.games.get(uuid).getPlayers());
		return new ResponseEntity<>(resp.toJSONString(), HttpStatus.OK); 
	}
	
	@RequestMapping(value = "/game/**", method=RequestMethod.POST, params = {TOKEN, LINE, COLUMN}, produces = "application/json")
	public ResponseEntity<String> postGameUuid(@PathVariable("uuid") String uuid, @RequestParam(TOKEN) String token, @RequestParam(LINE) String line, @RequestParam(COLUMN) String column) {
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
	
	
	@RequestMapping(value = "/game/**", method=RequestMethod.DELETE, params = {TOKEN}, produces = "application/json")
	public ResponseEntity<String> deleteGameUuid(@PathVariable("uuid") String uuid, @RequestParam(TOKEN) String token) {
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND); 
		}
		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}
		
		this.games.remove(uuid);
		return new ResponseEntity<>("deleted", HttpStatus.OK);
	}
	
	@RequestMapping(value = "/game/{uuid}/player", method=RequestMethod.POST, params = {TOKEN, PLAYER}, produces = "application/json")
	public ResponseEntity<String> addPlayer(@PathVariable("uuid") String uuid, @RequestParam(TOKEN) String token, @RequestParam(PLAYER) String player) {
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND); 
		}
		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}
		
		// check if player is already
		if (this.games.get(uuid).getPlayers().contains(player) == true) {
			return new ResponseEntity<>(String.format("Player %s its already being used", player), HttpStatus.BAD_REQUEST); 
		}
		
		this.games.get(uuid).players.put(player, token);
		return new ResponseEntity<>("done", HttpStatus.OK);
	}
	
	@RequestMapping(value = "/game/{uuid}/player", params = {TOKEN}, method=RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getPlayers(@PathVariable("uuid") String uuid, @RequestParam(TOKEN) String token) {
		if (this.games.containsKey(uuid) == false) {
			return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND); 
		}
		if (this.users.containsValue(token) == false) {
			return new ResponseEntity<>("invalid token", HttpStatus.UNAUTHORIZED);
		}
		
		return new ResponseEntity<>(this.games.get(uuid).getPlayerByToken(token) , HttpStatus.OK);
	}

	
}
