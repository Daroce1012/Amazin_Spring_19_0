package com.miw.presentation;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@SessionAttributes({ "loginCounter" })
@Controller
public class WelcomeController {

	/*
	 * Setting / as request mapping url we are setting the default controller for the application.
	 */
	
	@RequestMapping("/")
	public String welcome()
	{
		System.out.println("Executing Welcome controller");
		return "redirect:/private/menu";
	}
	
	@RequestMapping("error/403")
	public String error403() {
	    return "private/error";
	}

	@RequestMapping("/private/menu")
	public String index(Principal p, @ModelAttribute("loginCounter") LoginCounter loginCounter) {
		System.out.println("Executing private/menu controller for user "+ p);
		loginCounter.inc();
		return "private/index";
	}
	@RequestMapping("loginError")
	public String loginError(ModelMap model)
	{
		model.addAttribute("error","true");
		model.addAttribute("message", "login.error");
		return "login";
	}

	@RequestMapping("/loginForm")
	public String getForm(@RequestParam(value = "error", required = false) String error, ModelMap model) {
	    System.out.println("Preparing the model for Login");
	    if (error!=null && error.equals("wc")) {
	        model.addAttribute("errorMessage", "login.invalidCredentials");
	    }else if (error != null && !"wc".equals(error)) {
	    	 model.addAttribute("errorMessage", "login.unknownError");
	    }
	    return "login";
	}
	
	@ModelAttribute("loginCounter")
	public LoginCounter getLoginCounter() {
		System.out.println("Initializing loginCounter");
		return new LoginCounter();
	}

	
}
