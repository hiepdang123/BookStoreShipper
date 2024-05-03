package vnua.fita.bookstore.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import vnua.fita.bookstore.bean.Order;
import vnua.fita.bookstore.model.OrderDAO;
import vnua.fita.bookstore.util.Constant;
import vnua.fita.bookstore.util.MyUtil;

@WebServlet(urlPatterns = { "/shipperOrderList/delivering", "/shipperOrderList/delivered",
		"/shipperOrderList/failure" })
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, // 2MB
		maxFileSize = 1024 * 1024 * 10, // 10MB
		maxRequestSize = 1024 * 1024 * 20 // 20MB
)
public class ShipperHomeServlet extends HttpServlet {
	private OrderDAO orderDAO;

	public void init() {
		String jdbcURL = getServletContext().getInitParameter("jdbcURL");
		String jdbcPassword = getServletContext().getInitParameter("jdbcPassword");
		String jdbcUsername = getServletContext().getInitParameter("jdbcUsername");
		orderDAO = new OrderDAO(jdbcURL, jdbcUsername, jdbcPassword);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String servletPath = req.getServletPath();
		String pathInfo = MyUtil.getPathInfoFromServletPath(servletPath);
		String keyword = req.getParameter("keyword");
		req.setAttribute("keyword", keyword);
		RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/Views/shipperHomeView.jsp");
		dispatcher.forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<String> errors = new ArrayList<String>();
		String orderIdStr = req.getParameter("orderId");
		String confirmTypeStr = req.getParameter("confirmType");
		int orderId = -1;
		try {
			orderId = Integer.parseInt(orderIdStr);
		} catch (Exception e) {
			// TODO: handle exception
			errors.add(Constant.ORDER_ID_INVALID_VALIDATE_MSG);
		}
		byte confirmType = -1;
		try {
			confirmType = Byte.parseByte(confirmTypeStr);
		} catch (Exception e) {
			// TODO: handle exception
			errors.add(Constant.VALUE_INVALID_VALIDATE_MSG);
		}

		if (errors.isEmpty()) {
			boolean updateResult = false;
			if (Constant.DELEVERING_ORDER_STATUS == confirmType) {
				updateResult = orderDAO.updateOrderNo(orderId, confirmType);
			} else if (Constant.DELEVERED_ORDER_STATUS == confirmType) {
				updateResult = orderDAO.updateOrder(orderId, confirmType);
			} else if (Constant.REJECT_ORDER_STATUS == confirmType) {
				updateResult = orderDAO.updateOrder(orderId, confirmType);
			} else if (Constant.FAILURE_ORDER_STATUS == confirmType) {
				Part filePart = req.getPart("file");
				String fileName = UUID.randomUUID().toString() + "_" + MyUtil.getTimeLabel()
						+ MyUtil.extracFileExtension(filePart);
				String contextPath = getServletContext().getRealPath("/"); // Lấy đường dẫn thực của ứng dụng web
				String savePath = contextPath + "failure-img-upload"; // Đường dẫn đến thư mục 'img'

				File fileSaveDir = new File(savePath);
				if (!fileSaveDir.exists()) {
					fileSaveDir.mkdir(); // Tạo thư mục 'img' nếu nó không tồn tại
				}

				String filePath = savePath + File.separator + fileName; // Đường dẫn file cuối cùng để lưu trữ ảnh
				filePart.write(filePath); // Lưu file ảnh
				String imagePath = "failure-img-upload" + File.separator + fileName;
				
				updateResult = orderDAO.updateOrder(orderId, confirmType,imagePath);
			}
			if (updateResult) {
				req.setAttribute("message", Constant.UPDATE_ORDER_SUCCESS);
			} else {
				errors.add(Constant.UPDATE_ORDER_FAIL);
			}
		}
		if (!errors.isEmpty()) {
			req.setAttribute("errors", String.join(", ", errors));
		}
		doGet(req, resp);
	}
}
