package com.js11.p_cubs

class UploadReportedCubs {
    var id : String? = null
    var title: String? = null
    var description: String? = null
    var date: String? = null
    var time: String? = null
    var location: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var cub_imageURL: String? = null

    constructor(id1: String?, title1: String?, description1: String?, date1: String?, time1: String, location1: String?, latitude1: Double?, longitude1: Double?, cub_imageURL1: String?) {
        id = id1
        title = title1
        description = description1
        date = date1
        time = time1
        location = location1
        latitude = latitude1
        longitude = longitude1
        cub_imageURL = cub_imageURL1
    }
}