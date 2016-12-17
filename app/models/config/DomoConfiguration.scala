package models.config

abstract class DomoConfiguration(id: Int, name: String) {
  def getId = id
  def getName = name
}
