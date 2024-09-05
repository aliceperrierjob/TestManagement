package fr.uha.ensisa.ff.todo_auto.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.uha.ensisa.ff.todo_auto.dao.TodoDAO;
import fr.uha.ensisa.ff.todo_auto.dao.UnknownListException;
import fr.uha.ensisa.ff.todo_auto.dao.UnknownUserException;

@RestController
@RequestMapping("/api")
public class TasksController {
	
	@Autowired private TodoDAO dao;
	
	private String getUser() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}
	
	@ExceptionHandler(UnknownUserException.class)
	public void unknownUser(UnknownUserException x, HttpServletResponse rsp) throws IOException {
		SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);
        rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
	}
	
	// TODO properly handling missing list
//	@ExceptionHandler(UnknownListException.class)
//	public void unknownList(UnknownListException x, HttpServletResponse rsp) throws IOException {
//        rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "{ \"error\" : \"" + x.getMessage() + "\", \"unknownList\" : true }");
//	}

	@RequestMapping(value = "/main")
	public Map<String, Object> mainInfo(@RequestParam(required = false, defaultValue = "") String list) throws UnknownUserException {
		String user = getUser();
		
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("userName", user);
		
		ret.put("lists", dao.getLists(user));
		
		return ret;
	}

	@RequestMapping(value = "/lists")
	public Map<String /* ID */, String /* name */> lists() throws UnknownUserException {
		return dao.getLists(getUser());
	}

	@RequestMapping(value = "/lists/new", method = RequestMethod.POST)
	public String newList(@RequestParam(required = true) String name) throws UnknownUserException {
		return dao.createList(getUser(), name);
	}

	@RequestMapping(value = "/lists/{id:.*}", method = RequestMethod.DELETE)
	public void deleteList(@PathVariable String id) throws UnknownUserException, UnknownListException {
		dao.deleteList(getUser(), id);
	}

	@RequestMapping(value = "/lists/{id:.*}", method = RequestMethod.PUT)
	public void updateList(@PathVariable String id, @RequestBody Map<String, String> data) throws UnknownUserException, UnknownListException {
		if (!data.containsKey("name")) throw new IllegalArgumentException("Missing new name");
		dao.renameList(getUser(), id, data.get("name"));
	}

	@RequestMapping(value = "/tasks", method = RequestMethod.GET)
	public List<Map<String, Object>> tasks(@RequestParam(required = false, defaultValue = "") String list) throws UnknownUserException, UnknownListException {
		return list == null || (list = list.trim()).length() == 0 ?
				dao.getDefaultTasks(getUser()) :
				dao.getTasksOfList(getUser(), list);
	}

	@RequestMapping(value = "/tasks/new", method = RequestMethod.POST)
	public String newTask(@RequestParam(required = true) String name, @RequestParam(required = false, defaultValue = "") String list) throws UnknownUserException, UnknownListException {
		return list == null || (list = list.trim()).length() == 0 ?
				dao.createDefaultTask(getUser(), name) :
				dao.createListTask(getUser(), list, name);
	}

	@RequestMapping(value = "/tasks/{id:.*}", method = { RequestMethod.PUT})
	public void updateTask(@PathVariable String id, @RequestBody Map<String, String> data)
			throws UnknownUserException, UnknownListException {
		String list = data.getOrDefault("list", null);
		String task = data.get("task");
		if (task == null) throw new IllegalArgumentException("Missing name");
		if (list == null || (list = list.trim()).length() == 0 ) {
			dao.renameDefaultTask(getUser(), id, task);
		} else {
			dao.renameListTask(getUser(), list, id, task);
		}
	}

	@RequestMapping(value = "/tasks/done/{id:.*}", method = { RequestMethod.GET})
	public void setDoneTask(@PathVariable String id,
			@RequestParam(required = true) boolean done,
			@RequestParam(required = false, defaultValue = "") String list)
			throws UnknownUserException, UnknownListException {
				if (list == null || (list = list.trim()).length() == 0 ) {
					dao.setDefaultTaskDone(getUser(), id, done);
				} else {
					dao.setListTaskDone(getUser(), list, id, done);
				}
	}

	@RequestMapping(value = "/tasks", method = RequestMethod.DELETE)
	public void deleteTask(@RequestParam(required = true) String id, @RequestParam(required = false, defaultValue = "") String list)
			throws UnknownUserException, UnknownListException {
		if (list == null || (list = list.trim()).length() == 0 ) {
			dao.deleteDefaultTask(getUser(), id);
		} else {
			dao.deleteListTask(getUser(), list, id);
		}
	}
}
