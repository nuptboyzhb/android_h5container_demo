import React, { Component } from "react";
import { List } from "antd-mobile";
import ImageItemComponent from "./../ImageItemComponent/index";
import PropTypes from "prop-types";
import MyHttp from "./../../utils/http";
/**
 * 列表组件
 */
class ListContentComponent extends Component {
  static propTypes = {
    stories: PropTypes.array,
    onItemClick: PropTypes.func,
    hasMore: PropTypes.bool,
    handleInfiniteOnLoad: PropTypes.func,
  };

  constructor(props) {
    super(props);
    this.state = { stories: [], hasMore: true, loading: false, pageIndex: 1 };
  }

  componentWillMount() {
    console.log();
  }

  render() {
    //console.log(this.props.stories);
    if (
      this.state.stories.length === 0 &&
      this.props.stories &&
      this.props.stories.length > 0
    ) {
      this.state.stories = [...this.props.stories];
    }
    if (this.state.stories.length > 0) {
      return (
        <List renderFooter={this.renderFooter}>
          {this.state.stories.map((item) => {
            return (
              <ImageItemComponent
                key={item.id}
                data={item}
                onItemClick={this.props.onItemClick}
              />
            );
          })}
        </List>
      );
    }
    return <div></div>;
  }

  renderFooter = () => {
    return (
      <div ref={(footDom) => (this.footDom = footDom)}>
        {this.state.hasMore ? "loading..." : "加载完毕"}
      </div>
    );
  };

  componentDidMount() {
    window.addEventListener("scroll", () => {
      if (this.isInViewPortOfTwo(this.footDom)) {
        this.loadMore();
      }
    });
  }

  loadMore() {
    console.log("start loadmore");
    if (this.state.loading) {
      return;
    }
    this.state.loading = true;
    const http = new MyHttp();
    http.getTheDateMsg(this.state.pageIndex, (resp) => {
      console.log(resp);
      const newStories = [...this.state.stories].concat(resp.stories);
      const pageIndex = this.state.pageIndex + 1;
      this.setState({ stories: newStories, loading: false, pageIndex });
    });
  }

  isInViewPortOfTwo(el) {
    if (el) {
      const viewPortHeight =
        window.innerHeight ||
        document.documentElement.clientHeight ||
        document.body.clientHeight;
      const top = el.getBoundingClientRect() && el.getBoundingClientRect().top;
      //console.log(`top=${top},viewPortHeight = ${viewPortHeight}`);
      const result = top <= viewPortHeight;
      // if (result) {
      //   console.log("item scroll to bottom");
      // }
      return result;
    }
    return false;
  }
}

export default ListContentComponent;
