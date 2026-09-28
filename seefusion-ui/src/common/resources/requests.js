angular.module('resources.requests',[])

.factory('RequestService', ['$http', '$log', function ($http, $log) {

	var Requests = {};

	Requests.getRequests = function(type){
		var request = $http.get("/json/getrequests?type=" + type);
		return request.then(function(response){
			return response.data.pages;
		});
		
	};

	// type: active (running now), recent or slowest (since SeeFusion started)
	Requests.getQueries = function(type){
		var request = $http.get("/json/getqueries?type=" + type);
		return request.then(function(response){
			return response.data.queries;
		});
	};

	// One request and its queries; resolves to {error: ...} once it has left the request lists.
	Requests.getRequestQueries = function(pid){
		var request = $http.get("/json/getrequestqueries?pid=" + encodeURIComponent(pid));
		return request.then(function(response){
			return response.data;
		});
	};

	Requests.killRequest = function(pageID){
		var request = $http.post("/json/kill",{'pid':pageID});
		return request.then(function(response){
			return response.data;
		});
	};

	return Requests;

}]);