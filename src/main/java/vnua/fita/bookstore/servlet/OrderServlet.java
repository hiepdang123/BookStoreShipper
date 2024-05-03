package vnua.fita.bookstore.servlet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.google.gson.JsonObject;

import vnua.fita.bookstore.bean.Cart;
import vnua.fita.bookstore.bean.CartItem;
import vnua.fita.bookstore.bean.Order;
import vnua.fita.bookstore.bean.User;
import vnua.fita.bookstore.config.VNPayConfig;
import vnua.fita.bookstore.model.OrderDAO;
import vnua.fita.bookstore.util.Constant;
import vnua.fita.bookstore.util.MyUtil;

/**
 * Servlet implementation class OrderServlet
 */
@WebServlet("/order")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 20)
public class OrderServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private OrderDAO orderDAO;

	public void init() {
		String jdbcURL = getServletContext().getInitParameter("jdbcURL");
		String jdbcPassword = getServletContext().getInitParameter("jdbcPassword");
		String jdbcUsername = getServletContext().getInitParameter("jdbcUsername");
//		bookDAO = new BookDAO("jdbc:mysql://localhost:3306/bookstore", "root", "123456");
		orderDAO = new OrderDAO(jdbcURL, jdbcUsername, jdbcPassword);
	}

	public OrderServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String fowardPage = "/Views/cartView.jsp";
		String vnp_ResponseCode = req.getParameter("vnp_ResponseCode");
		boolean isSuccess = false;
		if (vnp_ResponseCode.equals("00")) {
			HttpSession session = req.getSession();
			String vnp_OrderInfo = req.getParameter("vnp_OrderInfo");

			Order order = (Order) session.getAttribute("order_vnpay");
			boolean insertResult = orderDAO.insertOrder(order);
			if (insertResult) {
				req.setAttribute(Constant.CART_OF_CUSTOMER, MyUtil.getCartOfCustomer(session));
				req.setAttribute(Constant.ORDER_OF_CUSTOMER, order);
				MyUtil.deleteCart(session);
				isSuccess = true;
				fowardPage = "/Views/detailOrderView.jsp";
			} else {
				req.setAttribute("errors", Constant.ADD_TO_CART_ACTION);
				fowardPage = "/Views/cartView.jsp";
				String vnp_RequestId = VNPayConfig.getRandomNumber(8);
				String vnp_Version = "2.1.0";
				String vnp_Command = "refund";
				String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
				String vnp_TransactionType = req.getParameter("trantype");
				String vnp_TxnRef = req.getParameter("order_id");
				int amount = (int) order.getTotalCost();
				String vnp_Amount = String.valueOf(amount);
				vnp_OrderInfo = "Hoan tien GD OrderId:" + vnp_TxnRef;
				String vnp_TransactionNo = ""; // Assuming value of the parameter "vnp_TransactionNo" does not exist on
												// your
												// system.
				String vnp_TransactionDate = req.getParameter("trans_date");
				String vnp_CreateBy = req.getParameter("user");

				Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
				SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
				String vnp_CreateDate = formatter.format(cld.getTime());

				String vnp_IpAddr = VNPayConfig.getIpAddress(req);

				JsonObject vnp_Params = new JsonObject();

				vnp_Params.addProperty("vnp_RequestId", vnp_RequestId);
				vnp_Params.addProperty("vnp_Version", vnp_Version);
				vnp_Params.addProperty("vnp_Command", vnp_Command);
				vnp_Params.addProperty("vnp_TmnCode", vnp_TmnCode);
				vnp_Params.addProperty("vnp_TransactionType", vnp_TransactionType);
				vnp_Params.addProperty("vnp_TxnRef", vnp_TxnRef);
				vnp_Params.addProperty("vnp_Amount", vnp_Amount);
				vnp_Params.addProperty("vnp_OrderInfo", vnp_OrderInfo);

				if (vnp_TransactionNo != null && !vnp_TransactionNo.isEmpty()) {
					vnp_Params.addProperty("vnp_TransactionNo", "{get value of vnp_TransactionNo}");
				}

				vnp_Params.addProperty("vnp_TransactionDate", vnp_TransactionDate);
				vnp_Params.addProperty("vnp_CreateBy", vnp_CreateBy);
				vnp_Params.addProperty("vnp_CreateDate", vnp_CreateDate);
				vnp_Params.addProperty("vnp_IpAddr", vnp_IpAddr);

				String hash_Data = String.join("|", vnp_RequestId, vnp_Version, vnp_Command, vnp_TmnCode,
						vnp_TransactionType, vnp_TxnRef, vnp_Amount, vnp_TransactionNo, vnp_TransactionDate,
						vnp_CreateBy, vnp_CreateDate, vnp_IpAddr, vnp_OrderInfo);

				String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hash_Data.toString());

				vnp_Params.addProperty("vnp_SecureHash", vnp_SecureHash);

				URL url = new URL(VNPayConfig.vnp_ApiUrl);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "application/json");
				con.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(con.getOutputStream());
				wr.writeBytes(vnp_Params.toString());
				wr.flush();
				wr.close();
				int responseCode = con.getResponseCode();
				System.out.println("nSending 'POST' request to URL : " + url);
				System.out.println("Post Data : " + vnp_Params);
				System.out.println("Response Code : " + responseCode);
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String output;
				StringBuffer response = new StringBuffer();
				while ((output = in.readLine()) != null) {
					response.append(output);
				}
				in.close();
			}
		}
		RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(fowardPage);
		dispatcher.forward(req, resp);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		List<String> errors = new ArrayList<String>();
		String deliveryAddress = req.getParameter("deliveryAddress");
		String paymentMode = req.getParameter("paymentMode");
		Part filePart = req.getPart("file");

		HttpSession session = req.getSession();

		validateOrderForm(deliveryAddress, paymentMode, filePart, errors);
		if (!errors.isEmpty()) {
			req.setAttribute("errors", errors);
			RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/Views/cartView.jsp");
			dispatcher.forward(req, resp);
			return;
		}
		if (Constant.CASH_PAYMENT_MODE.equals(paymentMode) || Constant.TRANSFER_PAYMENT_MODE.equals(paymentMode)) {
			Order order = createOrder(deliveryAddress, paymentMode, filePart, session);
			String fowardPage;
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
			RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(fowardPage);
			dispatcher.forward(req, resp);
		} 
		if (Constant.VNPAY_PAYMENT_MODE.equals(paymentMode)) {
			String vnp_Version = "2.1.0";
			String vnp_Command = "pay";
			String orderType = "other";
			String amout_str = req.getParameter("amount");
			String[] parts = amout_str.split("\\."); // Sử dụng \\ để tránh ký tự đặc biệt trong regex
			int amount = Integer.parseInt(parts[0]) * 100;
			String bankCode = req.getParameter("bankCode");

			String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
			String vnp_IpAddr = VNPayConfig.getIpAddress(req);

			String vnp_TmnCode = VNPayConfig.vnp_TmnCode;

			Map<String, String> vnp_Params = new HashMap<>();
			vnp_Params.put("vnp_Version", vnp_Version);
			vnp_Params.put("vnp_Command", vnp_Command);
			vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
			vnp_Params.put("vnp_Amount", String.valueOf(amount));
			vnp_Params.put("vnp_CurrCode", "VND");

			if (bankCode != null && !bankCode.isEmpty()) {
				vnp_Params.put("vnp_BankCode", bankCode);
			}
			vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
			vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang: " + vnp_TxnRef);
			vnp_Params.put("vnp_OrderType", orderType);

			String locate = req.getParameter("language");
			if (locate != null && !locate.isEmpty()) {
				vnp_Params.put("vnp_Locale", locate);
			} else {
				vnp_Params.put("vnp_Locale", "vn");
			}
			vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
			vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

			Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
			String vnp_CreateDate = formatter.format(cld.getTime());
			vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

			cld.add(Calendar.MINUTE, 15);
			String vnp_ExpireDate = formatter.format(cld.getTime());
			vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

			List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
			Collections.sort(fieldNames);
			StringBuilder hashData = new StringBuilder();
			StringBuilder query = new StringBuilder();
			Iterator<String> itr = fieldNames.iterator();
			while (itr.hasNext()) {
				String fieldName = (String) itr.next();
				String fieldValue = (String) vnp_Params.get(fieldName);
				if ((fieldValue != null) && (fieldValue.length() > 0)) {
					// Build hash data
					hashData.append(fieldName);
					hashData.append('=');
					hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
					// Build query
					query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
					query.append('=');
					query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
					if (itr.hasNext()) {
						query.append('&');
						hashData.append('&');
					}
				}
			}
			String queryUrl = query.toString();
			String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
			queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
			String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;
			Order order = createOrder(deliveryAddress, paymentMode, filePart, session);
			session.setAttribute("order_vnpay", order);
			resp.sendRedirect(paymentUrl);
//	        com.google.gson.JsonObject job = new JsonObject();
//	        job.addProperty("code", "00");
//	        job.addProperty("message", "success");
//	        job.addProperty("data", paymentUrl);
//	        Gson gson = new Gson();
//	        resp.getWriter().write(gson.toJson(job));
		}
	}

	private Map<Integer, CartItem> converListToMap(List<CartItem> orderBookList) {
		Map<Integer, CartItem> cartItemList = new HashMap<Integer, CartItem>();
		for (CartItem cartItem : orderBookList) {
			cartItemList.put(cartItem.getSelectedBook().getBookId(), cartItem);
		}
		return cartItemList;
	}

	private Order createOrder(String deliveryAddress, String paymentMode, Part filePart, HttpSession session)
			throws IOException {
		Order order = new Order();
		Date date = new Date();

		Cart cartOfCustomer = MyUtil.getCartOfCustomer(session);
		String customerName = MyUtil.getLoginedUser(session).getUsername();
		User customer = new User();
		customer.setUsername(customerName);
		order.setCustomer(customer);
		order.setDeliveryAddress(deliveryAddress);
		order.setPaymentMode(paymentMode);
		order.setOrderDate(date);
		order.setStatusDate(date);
		order.setTotalCost((int) cartOfCustomer.getTotalCost());
		order.setOrderBookList(new ArrayList<CartItem>(cartOfCustomer.getCartItemList().values()));
		if (Constant.CASH_PAYMENT_MODE.equals(paymentMode)) {
			order.setOrderStatus(Constant.DELEVERING_ORDER_STATUS);
			order.setOrderApproveDate(date);
			order.setPaymentStatus(false);
		} else if (Constant.TRANSFER_PAYMENT_MODE.equals(paymentMode)) {
			String fileName = customerName + "_" + MyUtil.getTimeLabel() + MyUtil.extracFileExtension(filePart);
			String contextPath = getServletContext().getRealPath("/"); // Lấy đường dẫn thực của ứng dụng web
			String savePath = contextPath + "transfer-img-upload"; // Đường dẫn đến thư mục 'img'

			File fileSaveDir = new File(savePath);
			if (!fileSaveDir.exists()) {
				fileSaveDir.mkdir(); // Tạo thư mục 'img' nếu nó không tồn tại
			}
			order.setOrderApproveDate(date);

			String filePath = savePath + File.separator + fileName; // Đường dẫn file cuối cùng để lưu trữ ảnh
			filePart.write(filePath); // Lưu file ảnh
			String imagePath = "transfer-img-upload" + File.separator + fileName;
			order.setOrderStatus(Constant.WAITING_CONFIRM_ORDER_STATUS);
			order.setPaymentStatus(false);
			order.setPaymentImagePath(imagePath);
		} else if (Constant.VNPAY_PAYMENT_MODE.equals(paymentMode)) {
			order.setOrderStatus(Constant.DELEVERING_ORDER_STATUS);
			order.setOrderApproveDate(date);
			order.setPaymentStatus(true);
		}
		return order;
	}

	private void validateOrderForm(String deliveryAddress, String paymentMode, Part filePart, List<String> errors) {
		if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
			errors.add(Constant.DELEVERY_ADDRESS_EMPTY_VALIDATE_MSG);
		}
		if (Constant.TRANSFER_PAYMENT_MODE.equals(paymentMode)) {
			if (filePart == null || filePart.getSize() < 0) {
				errors.add(Constant.TRANSFER_IMAGE_EMPTY_MSG);
			}
		}
	}

	private void refundVnpay() {

	}
}
