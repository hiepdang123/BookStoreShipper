package vnua.fita.bookstore.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import vnua.fita.bookstore.bean.CartItem;
import vnua.fita.bookstore.bean.Order;
import vnua.fita.bookstore.config.VNPayConfig;
import vnua.fita.bookstore.model.OrderDAO;
import vnua.fita.bookstore.util.Constant;
import vnua.fita.bookstore.util.MyUtil;

@WebServlet(urlPatterns = {"/pay"})
public class PayServlet extends HttpServlet{
	private static final long serialVersionUID = 1L;
	private OrderDAO orderDAO;

	public void init() {
		String jdbcURL = getServletContext().getInitParameter("jdbcURL");
		String jdbcPassword = getServletContext().getInitParameter("jdbcPassword");
		String jdbcUsername = getServletContext().getInitParameter("jdbcUsername");
//		bookDAO = new BookDAO("jdbc:mysql://localhost:3306/bookstore", "root", "123456");
		orderDAO = new OrderDAO(jdbcURL, jdbcUsername, jdbcPassword);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String vnp_ResponseCode=req.getParameter("vnp_ResponseCode");
		resp.sendRedirect(req.getContextPath()+"/order");
	}
	
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("1");
		String fowardPage = "/Views/cartView.jsp";
		String vnp_ResponseCode=req.getParameter("vnp_ResponseCode");
		if(vnp_ResponseCode=="00") {
			HttpSession session = req.getSession();

			Order order = (Order) session.getAttribute("order_vnpay");
			if (orderDAO.checkAndUpdateAvaiableBookOfOrder(order)) {
				boolean insertResult = orderDAO.insertOrder(order);
				if (insertResult) {
					req.setAttribute(Constant.CART_OF_CUSTOMER, MyUtil.getCartOfCustomer(session));
					req.setAttribute(Constant.ORDER_OF_CUSTOMER, order);
					MyUtil.deleteCart(session);
					fowardPage = "/Views/detailOrderView.jsp";
				} else {
					req.setAttribute("errors", Constant.ADD_TO_CART_ACTION);
					fowardPage = "/Views/cartView.jsp";
				}
			} else {
				MyUtil.updateCartOfCustomer(session, converListToMap(order.getOrderBookList()));
				fowardPage = "/Views/cartView.jsp";
			}
		}
		RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(fowardPage);
		dispatcher.forward(req, resp);
    }
	
	private Map<Integer, CartItem> converListToMap(List<CartItem> orderBookList) {
		Map<Integer, CartItem> cartItemList = new HashMap<Integer, CartItem>();
		for (CartItem cartItem : orderBookList) {
			cartItemList.put(cartItem.getSelectedBook().getBookId(), cartItem);
		}
		return cartItemList;
	}
}
