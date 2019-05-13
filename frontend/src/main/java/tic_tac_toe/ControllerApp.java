package tic_tac_toe;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ControllerApp {

	@GetMapping("/game/{uuid}")
	public String games(@PathVariable("uuid") String uuid, Model model) {
		model.addAttribute("uuid", uuid);
		return "game";
	}

	@GetMapping("/")
	public String games(Model model) {
		return "games";
	}

	@GetMapping("/login")
	public String login(Model model) {
		return "login";
	}
}
