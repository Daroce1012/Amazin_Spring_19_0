package com.miw.presentation;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.miw.business.bookmanager.BookManagerService;
import com.miw.model.Book;

@Controller
@RequestMapping("private/newBook")
public class NewBookController {
	@Autowired 
	BookManagerService bookManagerService;
	@Autowired
	
	public BookManagerService getBookManagerService() {
		return bookManagerService;
	}

	public void setBookManagerService(BookManagerService bookManagerService) {
		this.bookManagerService = bookManagerService;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public void get()
	{
		
	}

	@RequestMapping(method = RequestMethod.POST)
	public String newBook(@Valid @ModelAttribute Book book, BindingResult result )
	{
		if ( result.hasErrors())
		{
			return "private/newBook";
		}
		
		try {
			this.bookManagerService.newBook(book, 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "redirect:showBooks";
	}
	
	@ModelAttribute
	Book getBook()
	{
		return new Book();
	}
}
