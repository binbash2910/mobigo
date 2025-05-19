import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'authority',
    data: { pageTitle: 'mobigoApp.adminAuthority.home.title' },
    loadChildren: () => import('./admin/authority/authority.routes'),
  },
  {
    path: 'people',
    data: { pageTitle: 'mobigoApp.people.home.title' },
    loadChildren: () => import('./people/people.routes'),
  },
  {
    path: 'vehicle',
    data: { pageTitle: 'mobigoApp.vehicle.home.title' },
    loadChildren: () => import('./vehicle/vehicle.routes'),
  },
  {
    path: 'ride',
    data: { pageTitle: 'mobigoApp.ride.home.title' },
    loadChildren: () => import('./ride/ride.routes'),
  },
  {
    path: 'step',
    data: { pageTitle: 'mobigoApp.step.home.title' },
    loadChildren: () => import('./step/step.routes'),
  },
  {
    path: 'booking',
    data: { pageTitle: 'mobigoApp.booking.home.title' },
    loadChildren: () => import('./booking/booking.routes'),
  },
  {
    path: 'payment',
    data: { pageTitle: 'mobigoApp.payment.home.title' },
    loadChildren: () => import('./payment/payment.routes'),
  },
  {
    path: 'rating',
    data: { pageTitle: 'mobigoApp.rating.home.title' },
    loadChildren: () => import('./rating/rating.routes'),
  },
  {
    path: 'message',
    data: { pageTitle: 'mobigoApp.message.home.title' },
    loadChildren: () => import('./message/message.routes'),
  },
  /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
];

export default routes;
