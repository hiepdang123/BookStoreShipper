package vnua.fita.bookstore.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import vnua.fita.bookstore.bean.Order;
import vnua.fita.bookstore.model.OrderDAO;
import vnua.fita.bookstore.payload.DataResponse;
import vnua.fita.bookstore.util.Constant;

@WebServlet(urlPatterns = {"/api/order"})
public class APIOrderServlet extends HttpServlet {
	private OrderDAO orderDAO;
	private Gson gson = new Gson();

	public void init() {
		String jdbcURL = getServletContext().getInitParameter("jdbcURL");
		String jdbcPassword = getServletContext().getInitParameter("jdbcPassword");
		String jdbcUsername = getServletContext().getInitParameter("jdbcUsername");
		orderDAO = new OrderDAO(jdbcURL, jdbcUsername, jdbcPassword);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getParameter("status");
		String keyword = req.getParameter("keyword");
		String context=req.getContextPath();
		List<Order> orders = new ArrayList<Order>();
		if (Constant.DELEVERING_ACTION.equals(pathInfo)) {
			orders = orderDAO.getOrderList(Constant.DELEVERING_ORDER_STATUS, keyword,context);
			req.setAttribute("listType", "ĐANG CHỜ GIAO");
		} else if (Constant.DELEVERED_ACTION.equals(pathInfo)) {
			orders = orderDAO.getOrderList(Constant.DELEVERED_ORDER_STATUS, keyword,context);
			req.setAttribute("listType", "ĐÃ GIAO");
		} else if (Constant.FAILURE_ACTION.equals(pathInfo)) {
			orders = orderDAO.getOrderList(Constant.FAILURE_ORDER_STATUS, keyword,context);
			req.setAttribute("listType", "BỊ HỎNG");
		}

		DataResponse dataResponse = new DataResponse();
		dataResponse.setStatusCode(200);
		dataResponse.setMessage("");
		dataResponse.setData(orders);

		String dataJson = gson.toJson(dataResponse);

		PrintWriter out = resp.getWriter();
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		out.print(dataJson);
		out.flush();
	}
}
