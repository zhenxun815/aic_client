var vm = new Vue({
	el: '#vue',
	data() {
		return {
			loginOutShowStatus: false,  // 退出框显示状态
			deleteShowStatus: false,  // 删除框显示状态
			downShowStatus: false,  // 下载框显示状态
			downInfoShowStatus: false  // 下载框信息显示状态
		}
	},
	created() {

	},
	methods: {
		loginOutShow: function () { // 退出框显示状态切换
			this.loginOutShowStatus = !this.loginOutShowStatus
		},
		deleteShow: function () {  // 删除框显示状态切换
			this.deleteShowStatus = !this.deleteShowStatus
		},
		downShow: function () {  // 下载框显示切换
			this.downShowStatus = !this.downShowStatus
		},
		downInfoShow: function () {  // 下载框信息显示切换
			this.downInfoShowStatus = !this.downInfoShowStatus
		},
		deleteSure: function () {  // 再次确认是否删除
			var flag = confirm("确定要删除？")
			if (flag) {
				alert("是的")
			} else {
				this.deleteShow()
			}
		}
	}
})