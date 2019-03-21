// Navigation

.navbar {
  height: $height-nav;

  .navbar-brand {
    position: relative;
    z-index: 2;
    background: $color-bg;

    .logo {
      max-height: calc(100% - .1em);
    }
  }

  .navbar-menu {
    display: block;
    max-height: 0;
    transition: height .2s ease-out;
    position: relative;
    z-index: 1;
    overflow: hidden;
    // box-shadow: none;
    padding: 0;
  }
  input.nav-toggle:checked ~ .navbar-menu {
    display: block;
    max-height: 100vh;
  }
  input.nav-toggle:checked ~ .overlay {
    opacity: 1;
    pointer-events: all;
  }

  .navbar-start {
    position: relative;
  }

  .navbar-item {
    
    &:hover {
      color: #6C2A7E;
      font-weight: normal;
    }

    &.is-active {
      border-left-width: 8px;
      background-color: #eee;
      color:#6C2A7E;
      font-weight: bold;
    }

    label {
      display: block;
      cursor: pointer;
      margin: -8px -12px;
      padding: 8px 12px;

      &:hover {
        background: #f0f0f0;
      }
    }
  }

  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: $color-shadow;
    opacity: 0;
    transition: opacity .2s ease-out;
    pointer-events: none;
  }
}

/* Laptop > */
@media only screen and (min-width: 1088px) {
  .navbar.is-fixed-top {
    height: 100px;

    .container > .navbar-menu {
      display: flex;
      transform: translate(0);
      margin-left: 2em;
      max-height: none;
    }
   
    .navbar-item {
      padding-left: 2em;
      padding-right: 2em;

      &.is-active {
        border-left-width: 0;
        
        &:after {
          content: '';
          height: 8px;
          width: 100%;
          background: black;
          position: absolute;
          bottom: 0;
          left: 0;
        }
      }

      // Submenu
      & .navbar-menu {
        max-width: 0;
      }
    }
  }
}