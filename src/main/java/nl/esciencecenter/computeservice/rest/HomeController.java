package nl.esciencecenter.computeservice.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Home redirection to swagger api documentation 
 */
@Controller
public class HomeController {
	@RequestMapping(value = "/")
	public String index() {
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
		builder.pathSegment("swagger-ui/");
		return "redirect:" + builder.build().toString();
//		return "";
	}
	
	@RequestMapping(value = "/admin")
	public String admin() {
		return "redirect:admin/index.html";
	}
}
