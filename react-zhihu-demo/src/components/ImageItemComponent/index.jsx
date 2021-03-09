import React, { Component } from "react";
import "./index.css";
import PropTypes from "prop-types";

/**
 * item组件
 */
class ImageItemComponent extends Component {
  static propTypes = {
    data: PropTypes.object,
    onItemClick: PropTypes.func,
  };

  constructor(props) {
    super(props);
    this.state = {};
  }
  render() {
    return (
      <div className="bg-container">
        <div
          className="container"
          onClick={() => {
            this.props.onItemClick(this.props.data);
          }}
        >
          <img src={this.getImageUrl()} className="image" />
          <div className="container-div">
            <span className="text">{this.getTitle()}</span>
            <span className="hint">{this.getHint()}</span>
          </div>
        </div>
      </div>
    );
  }

  getImageUrl() {
    const images = this.props.data.images || [];
    if (images && images.length > 0) {
      return images[0];
    }
    return "";
  }

  getTitle() {
    return this.props.data.title;
  }

  getHint() {
    return this.props.data.hint;
  }
}

export default ImageItemComponent;
