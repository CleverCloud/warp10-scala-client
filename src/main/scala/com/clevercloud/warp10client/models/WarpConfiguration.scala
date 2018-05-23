package com.clevercloud.warp10client.models

case class WarpConfiguration(
  baseUrl: String
) {
  def pushUrl: String = s"$baseUrl/api/v0/update"
  def fetchUrl: String = s"$baseUrl/api/v0/fetch"
  def execUrl: String = s"$baseUrl/api/v0/exec"
}
