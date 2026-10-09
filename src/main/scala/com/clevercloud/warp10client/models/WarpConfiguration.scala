package com.clevercloud.warp10client.models

import org.apache.pekko.http.scaladsl.model.Uri

case class WarpConfiguration(baseUrl: String) {
  def pushUrl: String = s"$baseUrl/api/v0/update"
  def fetchUrl: String = s"$baseUrl/api/v0/fetch"
  def execUrl: String = s"$baseUrl/api/v0/exec"
}

object WarpConfiguration {
  def apply(baseUrl: Uri): WarpConfiguration = new WarpConfiguration(baseUrl.toString())
}
