(function(vc){
    //员工权限
    vc.extends({
        data:{
            unitInfo:{
                units:[],
                _currentFloorId:'',
            }
        },
        _initMethod:function(){

        },
        _initEvent:function(){
            vc.on('unit','loadUnit',function(_param){
                vc.component._loadUnits(_param);
            });
        },
        methods:{
            _loadUnits:function(_param){
                vc.component.staffPrivilegeInfo._currentFloorId=_param.floorId;
                var param = {
                    params:{
                        floorId:_param.floorId
                    }
                };
             //发送get请求
            vc.http.get('unit',
                         'loadUnit',
                          param,
                          function(json){
                             var _unitInfo = JSON.parse(json);
                             vc.component.unitInfo.units = _unitInfo.datas;

                          },function(){
                             console.log('请求失败处理');
                          });
            },
            _openDeleteUnitModel:function(_unit){
                _unit.floorId = vc.component.unitInfo._currentFloorId;
                vc.emit('deleteUnit','openUnitModel',_unit);
            }
        }
    });

})(window.vc);