package com.miw.presentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.miw.business.bookmanager.BookManagerService;

@Controller
public class ShowBooksController {

	@Autowired
	private BookManagerService bookManagerService;

	@RequestMapping("private/showBooks")
	public String seekBooks(Model model) throws Exception {
		System.out.println("Executing SeekBooks.");

		// We store the list of books in teh model.
		model.addAttribute("booklist",
				bookManagerService.getBooks());
		// We return the name of the view. 
		return "private/showBooks";
		
	}
}
