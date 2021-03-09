import React, { Component } from "react";
import { Carousel, WingBlank } from "antd-mobile";
import "./index.css";
import PropTypes from "prop-types";
/**
 * 头部组件
 */
class HeaderComponent extends Component {
  static propTypes = {
    data: PropTypes.array.isRequired,
    onItemClick: PropTypes.func,
  };

  constructor(props) {
    super(props);
    this.state = {};
    this.carousel = undefined;
  }

  onChange = (number) => {};

  dotStyle = {
    width: "5px",
    height: "5px",
    background: "#f00",
    color: " #ff0000",
  };

  dotActiveStyle = {
    width: "5px",
    height: "5px",
    background: "#fff",
    color: " #00ff00",
  };

  render() {
    if (this.props.data) {
      return (
        <WingBlank>
          <Carousel autoplay={true} afterChange={this.onChange} dots={true}>
            {this.props.data.map((item) => {
              return (
                <div
                  key={item.id}
                  className="item-container"
                  onClick={() => {
                    this.props.onItemClick(item);
                  }}
                >
                  <img key={item.id} src={item.image} className="img" alt="" />
                  <div className="center-container">
                    <span className="title">{item.title}</span>
                  </div>
                </div>
              );
            })}
          </Carousel>
        </WingBlank>
      );
    }
    return <span>loading...</span>;
  }

  componentDidMount() {
    console.log("componentDidMount");
  }
}

export default HeaderComponent;
