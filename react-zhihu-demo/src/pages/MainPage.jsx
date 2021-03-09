import React, { Component } from "react";
import HeaderComponent from "../components/HeaderComponent/index";
import ListContentComponent from "../components/ListContentComponent/index";
import "./mainpage.css";
import PropTypes from "prop-types";
import mockdata from "./../mockdata";

class MainPage extends Component {
  static propTypes = {
    data: PropTypes.object,
    hasMore: PropTypes.bool,
    handleInfiniteOnLoad: PropTypes.func,
  };

  static defaultProps = {
    data: mockdata,
  };

  constructor(props) {
    super(props);
    this.state = {};
  }

  onItemClick = (item) => {
    console.log("item = ", item);
    if (window.XJSBridge) {
      window.XJSBridge.callNative("openUrl", { url: item.url }, (resp) => {
        if (resp.success) {
          console.log("open in native");
        } else {
          window.location.href = item.url;
        }
      });
      return;
    } else {
      window.location.href = item.url;
    }
  };

  render() {
    console.log(this.props);
    if (this.props.data) {
      return (
        <div className="main-container">
          <HeaderComponent
            className="header-container"
            data={this.props.data.top_stories}
            onItemClick={this.onItemClick}
          />
          <ListContentComponent
            className="list-container"
            onItemClick={this.onItemClick}
            stories={this.props.data.stories}
            hasMore={this.props.hasMore}
            handleInfiniteOnLoad={this.props.handleInfiniteOnLoad}
          />
        </div>
      );
    }
    return <div />;
  }
}

export default MainPage;
