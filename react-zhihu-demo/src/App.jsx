import React, { Component } from "react";
import "./App.css";
import MainPage from "./pages/MainPage";
import Utils from "./utils/util";
import MyHttp from "./utils/http";

class App extends Component {
  constructor(props) {
    super(props);
    this.state = { data: {}, hasMore: true, loading: false };
  }

  componentWillMount() {
    const utils = new Utils();
    console.log(
      "device android:" + utils.isAndroid() + ",iOS:" + utils.isIOS()
    );
    const http = new MyHttp();
    http.getLatestMsg((data) => {
      this.setState({ data });
    });
  }

  componentDidMount() {}

  handleInfiniteOnLoad = () => {};

  render() {
    return (
      <div className="App">
        <MainPage
          data={this.state.data}
          hasMore={this.state.hasMore}
          handleInfiniteOnLoad={this.handleInfiniteOnLoad}
        />
      </div>
    );
  }
}

export default App;
