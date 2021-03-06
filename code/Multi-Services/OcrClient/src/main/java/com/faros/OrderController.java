package com.faros;

import com.faros.services.FoodService;
import com.faros.services.OrderService;
import com.faros.services.RestaurantService;
import com.faros.services.UserService;
import my.faros.model.Food;
import my.faros.model.Order;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by guang on 2017/4/28.
 */
@Controller
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);
    @Autowired
    private UserService userService;
    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private FoodService foodService;

    /*
    * This post method is used to create an order
    * and pass the content to order page after redirection
    * */
    @RequestMapping(value = "/restaurant/{name}/createOrder", method = RequestMethod.POST)
    public String createOrder(@PathVariable String name, @Valid String[] foods, @Valid String[] numbers, @Valid String tableId
            , RedirectAttributes redir) throws Exception {
        String order = "";
        ArrayList<String> list = new ArrayList<String>();
        for (String s : numbers) {
            if (!s.isEmpty() && !s.equals("0"))
                list.add(s);
        }
        int totalPrice = 0;
        for (int i = 0; i < foods.length; i++) {
            Food food = foodService.findFoodById(foods[i]);
            order += " " + food.getName() + " × " + list.get(i);
            totalPrice += food.getPrice();
        }
        log.info(order);
        redir.addFlashAttribute("content", order);
        redir.addFlashAttribute("tableId", tableId);
        redir.addFlashAttribute("totalPrice", totalPrice);
        return "redirect:/restaurant/" + name + "/order";
    }

    /*
    * This method returns order page with order content and other info;
    * Error handling for restaurant name error in url path.
    * */
    @RequestMapping(value = "/restaurant/{name}/order", method = RequestMethod.GET)
    public String order(@PathVariable String name, @ModelAttribute("content") final String content,
                        @ModelAttribute("tableId") final String tableId,@ModelAttribute("totalPrice") final int totalPrice, Model model) {
        JSONObject rest = restaurantService.findRestaurantByName(name);
        if (rest == null) {
            return "redirect:/error";
        } else {
            model.addAttribute("content", content);
            model.addAttribute("tableId", tableId);
            model.addAttribute("totalPrice", totalPrice);
            model.addAttribute("restaurantName", rest.getAsString("name"));
            return "order";
        }
    }

    /*
    * This post method is used to save an order with comment to remote server and database
    * and redirect to orderStatus page corresponding to the tableId
    * */
    @RequestMapping(value = "/restaurant/saveOrder", method = RequestMethod.POST)
    public String saveOrder(@Valid String content,@Valid String restaurantName,@Valid int tableId,@Valid int totalPrice,@Valid String comment){
        Order newOrder = new Order();
        newOrder.setContent(content);
        newOrder.setTableId(tableId);
        newOrder.setTotalPrice(totalPrice);
        JSONObject rest = restaurantService.findRestaurantByName(restaurantName);
        newOrder.setRestaurantId(Integer.parseInt(rest.getAsString("id")));
        newOrder.setComments(comment);
        orderService.save(newOrder);
        return "redirect:/restaurant/"+ restaurantName+"/orderStatus/"+tableId;
    }

    /*
    * This method returns orderStatus page with all orders and total price to that tableId
    * Error handling for restaurant name error in url path.
    * */
    @RequestMapping(value = "/restaurant/{name}/orderStatus/{tableId}", method = RequestMethod.GET)
    public String orderStatus(@PathVariable String name, @PathVariable int tableId, Model model) throws IOException {
        JSONObject rest = restaurantService.findRestaurantByName(name);
        if (rest == null) {
            return "redirect:/error";
        } else {
            List<JSONObject> orders = orderService.findAll(Integer.parseInt(rest.getAsString("id")));
            List<JSONObject> myOrders = new ArrayList<>();
            int price = 0;
            for(JSONObject order:orders){
                if(Integer.parseInt(order.getAsString("tableId"))==tableId){
                    myOrders.add(order);
                    price += Integer.parseInt(order.getAsString("totalPrice"));
                }
            }
            model.addAttribute("tableId", tableId);
            model.addAttribute("price", price);
            model.addAttribute("restaurantName", name);
            model.addAttribute("orders", myOrders);
            return "orderStatus";
        }
    }

    /*
    * This method returns orderOverview page with all unfinished orders;
    * Only accessible by users with role "manager" and "employee"
    * */
    @RequestMapping(value = "/restaurant/orderOverview", method = RequestMethod.GET)
    public String orderOverview(Model model){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
        JSONObject userExists = userService.findByUsername(user.getUsername());
        JSONObject rest = restaurantService.findRestaurantById(Integer.parseInt(userExists.getAsString("restaurantId")));
        model.addAttribute("restaurantName", rest.getAsString("name"));
        List<JSONObject> orders = null;
        try {
            orders = orderService.findAll(Integer.parseInt(rest.getAsString("id")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        model.addAttribute("orders", orders);
        return "orderOverview";
    }

    /*
    * This post method changes an order status and save it to remove server;
    * Only accessible by users with role "manager" and "employee"
    * */
    @RequestMapping(value = "/restaurant/order/changeStatus", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void changeStatus(@Valid long id, String status) {
        log.info("Here comes in changeStatus: id {} and new status {}",id,status);
        orderService.changeOrderStatus(id,status);
    }

    /*
    * This method returns now-to-serve page with all ready orders;
    * Only accessible by users with role "manager" and "employee"
    * */
    @RequestMapping(value = "/restaurant/now-to-serve", method = RequestMethod.GET)
    public String nowToServe(Model model){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
        JSONObject userExists = userService.findByUsername(user.getUsername());
        JSONObject rest = restaurantService.findRestaurantById(Integer.parseInt(userExists.getAsString("restaurantId")));
        model.addAttribute("restaurantName", rest.getAsString("name"));
        List<JSONObject> orders = new ArrayList<>();
        List<JSONObject> result = new ArrayList<>();
        try {
            orders = orderService.findAll(Integer.parseInt(rest.getAsString("id")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(JSONObject order:orders){
            if(order.getAsString("status").equals("Ready")){
                result.add(order);
            }
        }
        model.addAttribute("orders", result);
        return "now-to-serve";
    }
}
