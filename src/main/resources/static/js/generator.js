$(function () {
    $("#jqGrid").jqGrid({
        url: 'sys/generator/list',
        beforeProcessing: function(res) {
          if(res.code === 500) {
              alert(res.msg);

              return false;
          }
         },
        datatype: "json",
        colModel: [			
			{ label: '表名', name: 'tableName', width: 100, key: true },
			{ label: 'Engine', name: 'engine', width: 70},
			{ label: '表备注', name: 'tableComment', width: 100 },
			{ label: '创建时间', name: 'createTime', width: 100 }
        ],
		viewrecords: true,
        height: 385,
        rowNum: 100,
		rowList : [10,30,50,100,200],
        rownumbers: true, 
        rownumWidth: 25, 
        autowidth:true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader : {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames : {
            page:"page", 
            rows:"limit",
            order: "order"
        },
        gridComplete:function(){
        	//隐藏grid底部滚动条
        	$("#jqGrid").closest(".ui-jqgrid-bdiv").css({ "overflow-x" : "hidden" }); 
        }
    });
});

var vm = new Vue({
	el:'#ftrapp',
	data:{
		q:{
			tableName: null,
            coon: 'jdbc:mysql://39.98.186.143:3306/bjzx_icbc',
            user: 'root',
            pwd: 'deepchain@20190115'
		},
        s: {
            moduleName: null,
            author: null,
            email:null
        }
	},
	methods: {
		query: function () {
			$("#jqGrid").jqGrid('setGridParam',{
                postData:{'tableName': vm.q.tableName,'coon':vm.q.coon,'user':vm.q.user,'pwd':vm.q.pwd},
                page:1 
            }).trigger("reloadGrid");
		},
		generator: function() {
			var tableNames = getSelectedRows();
			if(tableNames == null){
				return ;
			}
            location.href = "sys/generator/code?coon="+this.q.coon+"&user="+this.q.user+"&pwd="+this.q.pwd 
            + "&moduleName=" +  this.s.moduleName
            + "&author=" +  this.s.author
            + "&email=" +  this.s.email
            +"&tables=" + JSON.stringify(tableNames);
		}
	}
});

