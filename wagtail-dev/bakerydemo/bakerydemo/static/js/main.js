const navbarElement = document.getElementById('navbar');
let isCompact = false;

function resizeNavbar() {
  
  // Toggle to small
  if(window.scrollY > 200 && !isCompact) {
    navbarElement.classList.add('is-compact');
    isCompact = true;
  }

  // Toggle to large
  if(window.scrollY < 200 && isCompact) {
    navbarElement.classList.remove('is-compact');
    isCompact = false;
  }
}

window.addEventListener('scroll', resizeNavbar);