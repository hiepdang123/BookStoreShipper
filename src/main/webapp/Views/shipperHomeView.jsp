<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="vnua.fita.bookstore.util.Constant"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet"
	href="${pageContext.request.contextPath }/css/bookstore_style.css">
<title>Trang chủ phía admin</title>
</head>
<body>
	<jsp:include page="_header_shipper.jsp"></jsp:include>
	<jsp:include page="_menu_shipper.jsp"></jsp:include>
	<div align="center">
		<h3>DANH SÁCH ĐƠN HÀNG ${listType }</h3>
		<input type="text" name="keyword" id="keyword"
			placeholder="Tìm mã hóa đơn" />
		<button id="searchButton">Tìm kiếm</button>
		<form id="shipperOrderForm" method="POST" action=""
			enctype="multipart/form-data">
			<input type="hidden" name="orderId" id="orderIdOfAction" /> <input
				type="hidden" name="confirmType" id="confirmTypeOfAction" /> <input
				type="file" name="file" id="fileImage" hidden />
		</form>
		<p style="color: red;">${errors }</p>
		<p style="color: blue;">${message }</p>
		<table border="1" id="data-container">
		</table>
	</div>
	<jsp:include page="_footer.jsp"></jsp:include>
	<script type="text/javascript">
	function onClickAdminOrderConfirm(orderId, confirmType, action) {
	    var form = document.getElementById("shipperOrderForm");
	    
	    // Gán giá trị orderId và confirmType vào các input hidden trong form
	    document.getElementById("orderIdOfAction").value = orderId;
	    document.getElementById("confirmTypeOfAction").value = confirmType;
	    
	    // Lấy ra input chứa file
	    var fileInput = document.getElementById("fileImage");
	    
	    // Kiểm tra xem có file đã được chọn chưa
	    if (fileInput.files.length > 0) {
	        // Gán file đã chọn vào form
	        form.append("file", fileInput.files[0]);
	    }
	    
	    // Gán giá trị action cho form
	    form.action = action;
	    
	    // Gửi form đi
	    form.submit();
	}

	function loadImageFailure(event,id) {
	    let output = document.getElementById(id);
	    output.src = URL.createObjectURL(event.target.files[0]);
	    output.onload = function() {
	        URL.revokeObjectURL(output.src)
	    }
	    document.getElementById("fileImage").files = event.target.files;
	}
	
	</script>
	<script type="text/javascript">
	const getListOrder = (url) => {
	    return fetch(url)
	        .then(response => response.json())
	        .then(data => {
	            // Xử lý dữ liệu trả về từ API (data.data)
	            return data.data;
	        });
	};
	
	const Constant = {
		    TRANSFER_PAYMENT_MODE: 'transfer',
		    WAITING_CONFIRM_ORDER_STATUS: 1,
		    DELEVERING_ORDER_STATUS: 2,
		    DELEVERED_ORDER_STATUS: 3,
		    FAILURE_ORDER_STATUS: 7,
		    WAITING_APPROVE_ACTION: 'waiting',
		    DELEVERING_ACTION: 'delivering',
		    FAILURE_ACTION: 'failure'
		};
	
	const renderData = (orders) => {
		const table = document.getElementById("data-container");  
		let html = "\
		    <tr>\
		        <th>Mã hóa đơn</th>\
		        <th>Tên khách</th>\
		        <th>Số điện thoại</th>\
		        <th>Ngày đặt mua</th>\
		        <th>Ngày xác nhận</th>\
		        <th>Địa chỉ nhận sách</th>\
		        <th>Phương thức thanh toán</th>\
		        <th>Trạng thái đơn hàng</th>\
		        <th>Thao tác</th>\
		    </tr>\
		";
		orders.forEach(orderOfCustomer => {
		    // Định dạng ngày tháng từ timestamp
		    const orderDate = new Date(orderOfCustomer.orderDate);
		    const formattedOrderDate = orderDate.getDate() + '-' + (orderDate.getMonth() + 1) + '-' + orderDate.getFullYear() + ' ' + orderDate.getHours() + ':' + orderDate.getMinutes();

		    const approveDate = new Date(orderOfCustomer.orderApproveDate);
		    const formattedApproveDate = approveDate.getDate() + '-' + (approveDate.getMonth() + 1) + '-' + approveDate.getFullYear() + ' ' + approveDate.getHours() + ':' + approveDate.getMinutes();
		    
		    html += "\
		        <tr>\
		            <td>" + orderOfCustomer.orderNo + "</td>\
		            <td>" + orderOfCustomer.customer.fullname + "</td>\
		            <td>" + orderOfCustomer.customer.mobile + "</td>\
		            <td>" + formattedOrderDate + "</td>\
		            <td>" + formattedApproveDate + "</td>\
		            <td>" + orderOfCustomer.deliveryAddress + "</td>\
		            <td>" + orderOfCustomer.paymentModeDescription;
		    if (orderOfCustomer.paymentMode === Constant.TRANSFER_PAYMENT_MODE) {
		        html += "\
		                <br/>\
		                <button onclick=\"document.getElementById('divImg" + orderOfCustomer.orderId + "').style.display='block'\">Xem chi tiết</button>\
		                <button onclick=\"document.getElementById('divImg" + orderOfCustomer.orderId + "').style.display='none'\">Ẩn</button>\
		                <div id=\"divImg" + orderOfCustomer.orderId + "\" style=\"display: none; padding-top: 5px;\">\
		                    <img alt=\"Transfer Image\" src=\"" + orderOfCustomer.paymentImagePath + "\" width=\"150\"/>\
		                </div>";
		    }
		    html += "\
		            </td>\
		            <td>" + orderOfCustomer.orderStatusDescription;
		    if (Constant.WAITING_CONFIRM_ORDER_STATUS !== orderOfCustomer.orderStatus) {
		        html += "&nbsp;-&nbsp;" + orderOfCustomer.paymentStatusDescription;
		    }
		    html += "\
		            </td>\
		            <td>\
		                <button onclick=\"document.getElementById('div" + orderOfCustomer.orderId + "').style.display='block'\">Xem chi tiết</button>\
		                <button onclick=\"document.getElementById('div" + orderOfCustomer.orderId + "').style.display='none'\">Ẩn</button>\
		                <div id=\"div" + orderOfCustomer.orderId + "\" style=\"display: none;\">\
		                    <h3>Các cuốn sách trong hóa đơn</h3>\
		                    <table border=\"1\">\
		                        <tr style=\"background: yellow;\">\
		                            <th>Tiêu đề</th>\
		                            <th>Tác giả</th>\
		                            <th>Giá tiền</th>\
		                            <th>Số lượng mua</th>\
		                            <th>Tổng thành phần</th>\
		                        </tr>";
		    orderOfCustomer.orderBookList.forEach(cartItem => {
		        html += "\
		            <tr>\
		                <td>" + cartItem.selectedBook.title + "</td>\
		                <td>" + cartItem.selectedBook.author + "</td>\
		                <td>" + cartItem.selectedBook.price + "<sup>đ</sup></td>\
		                <td>" + cartItem.quantity + "</td>\
		                <td>" + (cartItem.selectedBook.price * cartItem.quantity) + "<sup>đ</sup></td>\
		            </tr>";
		    });
		    html += "\
		                    </table>\
		                    <br/>Tổng số tiền: <b>" + orderOfCustomer.totalCost + "<sup>đ</sup></b>";
		    if (Constant.WAITING_CONFIRM_ORDER_STATUS === orderOfCustomer.orderStatus) {
		        html += "&nbsp;&nbsp;&nbsp;&nbsp;\
		                <button onclick=\"onClickAdminOrderConfirm(" + orderOfCustomer.orderId + "," + Constant.DELEVERING_ORDER_STATUS + ",'" + Constant.WAITING_APPROVE_ACTION + "')\">Xác nhận đơn</button>";
		    } 
		    console.log(Constant.DELEVERING_ORDER_STATUS === orderOfCustomer.orderStatus," - ",orderOfCustomer.orderStatus," - ",Constant.DELEVERING_ORDER_STATUS)
 			if (Constant.DELEVERING_ORDER_STATUS === orderOfCustomer.orderStatus) {
		        html += "<br/><br/>\
		                <img alt=\"\" src=\"\" id=\"bookImage" + orderOfCustomer.orderId + "\" width=\"150\">&nbsp;\
		                <input type=\"file\" name=\"file\" accept=\"image/*\" onchange=\"loadImageFailure(event,'bookImage" + orderOfCustomer.orderId + "')\"/>\
		                <br/>\
		                <button onclick=\"onClickAdminOrderConfirm(" + orderOfCustomer.orderId + "," + Constant.DELEVERED_ORDER_STATUS + ",'" + Constant.DELEVERING_ACTION + "')\">Xác nhận đã giao đơn hàng</button>\
		                <button onclick=\"onClickAdminOrderConfirm(" + orderOfCustomer.orderId + "," + Constant.FAILURE_ORDER_STATUS + ",'" + Constant.FAILURE_ACTION + "')\">Xác nhận khách trả hàng</button>";
		    } 
			if (Constant.FAILURE_ORDER_STATUS === orderOfCustomer.orderStatus) {
		        html += "&nbsp;&nbsp;&nbsp;&nbsp;\
		                <img src=\"" + orderOfCustomer.failureImagePath + "\" width=\"150\" height=\"150\"/>";
		    }
		    html += "\
		                </div>\
		            </td>\
		        </tr>";
		});
		table.innerHTML = html;

	}
	var lastPart="";
	var keyword="";
	window.onload = async()=> {
	    var url = window.location.href;
	    var parts = url.split("/");
	    lastPart = parts[parts.length - 1];
	    
	    let order = await getListOrder("http://localhost:8082/ShipperBookStore/api/order?status=" + lastPart);
	    renderData(order)
	};
	const button = document.getElementById("searchButton"); 
	button.addEventListener("click", async() => {
		const keywordInput = document.getElementById("keyword"); 
		const keyword = keywordInput.value.trim();
		let order = await getListOrder("http://localhost:8082/ShipperBookStore/api/order?status="+ lastPart+"&keyword="+keyword);
		renderData(order)
	})
	</script>
</body>
</html>