package idv.henry.web.controller;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class BrigeController {
	@RequestMapping("/")
	public String index(Model model) {
		
		model.addAttribute("hello", "Hello World!!!");
		return "index";
	}
	
	@RequestMapping("/hello")
	public String getHello(Model model) {
		System.out.println("hello");
		model.addAttribute("data", "Hello Thymeleaf");  
		return "index";
	}

	@RequestMapping(value = "/index")
	public String index2(Model model) {
		model.addAttribute("hello", "Hello World!!!");
		model.addAttribute("data", "Hello Thymeleaf");  
		return "index";
	}
	
	@GetMapping("/todos")
	public ModelAndView todoList() {
		HashMap<String, Integer> todos = new HashMap<>();
		 
        // Adding elements to the Map
        // using standard add() method
		todos.put("vishal", 10);
		todos.put("sachin", 30);
		todos.put("vaibhav", 20);
	    

	    // create a new `ModelAndView` object
	    ModelAndView mav = new ModelAndView("index");
	    mav.addObject("todos", todos);

	    return mav;
	}
	
	@RequestMapping(value = "/json", method=RequestMethod.POST)
	public String getJson(@RequestParam HashMap<String, String> parameters) {
		System.out.println(parameters.toString());
		return "index";
	}
	
//	@RequestMapping(value = "/json", method=RequestMethod.POST)
//	public String getJson(@RequestParam String message) {
//		System.out.println(message);
//		return "index";
//	}
}
